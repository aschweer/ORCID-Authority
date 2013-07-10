package org.dspace.authority.orcid;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.log4j.Logger;
import org.dspace.content.DCPersonName;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz at OR13 dev challenge
 */
public class OrcidPersonLookup implements ChoiceAuthority {

    private static Logger log = Logger.getLogger(OrcidPersonLookup.class);

    /**
     * Get all values from the authority that match the profferred value.
     * Note that the offering was entered by the user and may contain
     * mixed/incorrect case, whitespace, etc so the plugin should be careful
     * to clean up user data before making comparisons.
     *
     * Value of a "Name" field will be in canonical DSpace person name format,
     * which is "Lastname, Firstname(s)", e.g. "Smith, John Q.".
     *
     * Some authorities with a small set of values may simply return the whole
     * set for any sample value, although it's a good idea to set the
     * defaultSelected index in the Choices instance to the choice, if any,
     * that matches the value.
     *
     * @param field being matched for
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start choice at which to start, 0 is first.
     * @param limit maximum number of choices to return, 0 for no limit.
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale) {
        DCPersonName name = new DCPersonName(text);
        if (name.getFirstNames() == null && name.getLastName() == null) {
            return new Choices(true);
        }
        
        Choice[] values;
        try {
            values = doQuery("http://pub.orcid.org/search/orcid-bio/", name.getFirstNames(), name.getLastName());
        } catch (LookupException e) {
            log.error("Cannot look up matches for field=" + field + ", text=" + text, e);
            return new Choices(true);
        }
        Choice[] offsetValues = values;
        if (start > 0 || limit > 0) {
            offsetValues = doOffset(values, start, limit);
        }

        if (offsetValues.length == 0) {
            return new Choices(Choices.CF_NOTFOUND);
        }
        
        int confidence;
        int defaultSelected = -1;
        if (offsetValues.length > 1) {
            confidence = Choices.CF_AMBIGUOUS;
            defaultSelected = 0;
        } else {
            confidence = Choices.CF_UNCERTAIN;
        }
        boolean hasMore = start + offsetValues.length < values.length;
        return new Choices(offsetValues, start, values.length, confidence, hasMore, defaultSelected);
    }

    Choice[] doOffset(Choice[] values, int start, int limit) {
        List<Choice> result = new ArrayList<Choice>();
        if (start < values.length) {
            for (int i = start; i < values.length; i++) {
                if (limit > 0 && result.size() >= limit) {
                    break;
                }
                result.add(values[i]);
            }
        }
        return result.toArray(new Choice[result.size()]);
    }

    Choice[] doQuery(String baseUrl, String firstName, String lastName) throws LookupException {
        List<Choice> results;

	    NameValuePair[] args = new NameValuePair[1];

	    StringBuilder query = new StringBuilder();
	    query.append("q=");
	    query.append("family-name:");
	    query.append(lastName);
	    query.append("+AND+given-names:");
	    query.append(firstName);

	    Document doc = makeRequest(baseUrl, query.toString());
	    results = readChoiceList(doc);
        
        return results.toArray(new Choice[results.size()]);
    }

	private Document makeRequest(String baseUrl, String paramsString) throws LookupException {
		Document doc;
		log.info("About to look up " + baseUrl + paramsString);
		GetMethod method = new GetMethod(baseUrl);
		method.setQueryString(paramsString);

		try {
			HttpClient client = new HttpClient();
			client.getHttpConnectionManager().getParams().setConnectionTimeout(10000); // 10 second timeout to allow for look-up
		    client.executeMethod(method);
		    InputStream responseStream = method.getResponseBodyAsStream();

		    if (responseStream == null) {
		        throw new LookupException("No response from lookup service");
		    }

			log.info("About to create choice list from response");


			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(responseStream);
			} catch (ParserConfigurationException e) {
				throw new LookupException("Problem reading lookup response", e);
			} catch (SAXException e) {
				throw new LookupException("Problem reading lookup response", e);
			} catch (IOException e) {
				throw new LookupException("Problem reading lookup response", e);
			} finally {
				try {
					responseStream.close();
				} catch (IOException e) {
					log.warn("Exception caught while closing response stream, ignoring", e);
				}
			}
		} catch (IOException ioe) {
		    throw new LookupException("Cannot use lookup service", ioe);
		} finally {
		    method.releaseConnection();
		}
		return doc;
	}

	String doReverseQuery(String baseUrl, String key) throws LookupException {
		NameValuePair[] args = new NameValuePair[1];
		args[0] = new NameValuePair("id", key);
		String paramsString = EncodingUtil.formUrlEncode(args, "UTF-8");
		log.info("About to look up " + baseUrl + paramsString);

		Document doc = makeRequest(baseUrl, paramsString);
		return readLabel(doc);
	}

	String readLabel(Document doc) throws LookupException {
		String result = null;
		if (doc == null) {
			throw new LookupException("Cannot read lookup service response");
		}
		NodeList names = doc.getElementsByTagName("displayname");
		if (names.getLength() > 0) {
			result = names.item(0).getTextContent();
		}
		return result;
	}

	List<Choice> readChoiceList(Document doc) throws LookupException {
        List<Choice> results = new ArrayList<Choice>();
        if (doc == null) {
            throw new LookupException("Cannot read lookup service response");
        }

        NodeList authors = doc.getElementsByTagName("author");
        for (int i = 0; i < authors.getLength(); i++) {
            Node author = authors.item(i);

            Choice choice = createChoiceFromAuthor(author);

            if (choice != null) {
                results.add(choice);
            }
        }
        return results;
    }

    Choice createChoiceFromAuthor(Node author) {
        String id = null;
        String name = null;
        String role = null;
        String phone = null;

        NodeList children = author.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (child.getNodeName().equals("id")) {
                    id = child.getTextContent();
                } else if (child.getNodeName().equals("name")) {
                    name = child.getTextContent();
                } else if (child.getNodeName().equals("role")) {
                    role = child.getTextContent();
                } else if (child.getNodeName().equals("phone")) {
                    phone = child.getTextContent();
                }
            }
        }

        Choice choice = null;
        if (id != null && name != null) {
            choice = new Choice();

            choice.authority = id;
            choice.value = name;

            StringBuilder labelBuilder = new StringBuilder(name);
            if (role != null) {
                labelBuilder.append(" (");
                labelBuilder.append(role);
                labelBuilder.append(")");
            }
            if (phone != null) {
                labelBuilder.append(" phone: ");
                labelBuilder.append(phone);
            }
            choice.label = labelBuilder.toString();
        }
        return choice;
    }


    /**
     * Get the single "best" match (if any) of a value in the authority
     * to the given user value.  The "confidence" element of Choices is
     * expected to be set to a meaningful value about the circumstances of
     * this match.
     *
     * This call is typically used in non-interactive metadata ingest
     * where there is no interactive agent to choose from among options.
     *
     * @param field being matched for
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     */
    public Choices getBestMatch(String field, String text, int collection, String locale) {
        Choices allMatches = getMatches(field, text, collection, 0, 0, locale);
        Choices result;
        if (allMatches.isError()) {
            result = new Choices(true);
        } else if (allMatches.total == 0) {
            result = new Choices(new Choice[0], 0, 0, Choices.CF_NOTFOUND, false);
        } else {
            Choice[] value = new Choice[] { allMatches.values[0] };
            int confidence = allMatches.total > 1 ? Choices.CF_AMBIGUOUS : Choices.CF_UNCERTAIN;
            result = new Choices(value, 0, allMatches.total, confidence, allMatches.total > 1);
        }
        return result;
    }

    /**
     * Get the canonical user-visible "label" (i.e. short descriptive text)
     * for a key in the authority.  Can be localized given the implicit
     * or explicit locale specification.
     *
     * This may get called many times while populating a Web page so it should
     * be implemented as efficiently as possible.
     *
     * @param field being matched for
     * @param key authority key known to this authority.
     * @param locale explicit localization key if available, or null
     * @return descriptive label - should always return something, never null.
     */
    public String getLabel(String field, String key, String locale) {
	/*if (reverseUrl == null || "".equals(reverseUrl)) {
		    return key;
	    }

	    try {
		    String name = doReverseQuery(reverseUrl, key);
		    if (name != null && !"".equals(name.trim())) {
			    return name;
		    }
	    } catch (LookupException e) {
		    log.error("Could not get label for key=" + key, e);
		    }*/
	    return key; // fall back to key in case of error
	    }

}
