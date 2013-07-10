package org.dspace.authority.orcid;

import junit.framework.TestCase;
import org.dspace.content.authority.Choice;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for LCoNZ
 */
public class LookupTest extends TestCase {

    private OrcidPersonLookup authorLookup;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        authorLookup = new OrcidPersonLookup();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        authorLookup = null;
    }
    /*
    public void testDoOffset() {
        List<Choice> testList = new ArrayList<Choice>();
        int testListInitialSize = 20;
        for (int i = 0; i < testListInitialSize; i++) {
            Choice choice = new Choice();
            choice.authority = "" + i;
            choice.label = "" + i;
            choice.value= "" + i;
            testList.add(choice);
        }
        Choice[] testChoices = testList.toArray(new Choice[testList.size()]);
        assertEquals("Test list should have " + testListInitialSize + " elements", testListInitialSize, testChoices.length);

        Choice[] offsetChoices = authorLookup.doOffset(testChoices, 0, 0);
        assertTrue("Start 0, offset 0 shouldn't change the array", Arrays.deepEquals(testChoices, offsetChoices));

        /* limit tests * /
        int testLimit = 5;
        offsetChoices = authorLookup.doOffset(testChoices, 0, testLimit);
        assertEquals("Limit appropriately limits number of elements", testLimit, offsetChoices.length);

        for (int i = 0; i < testLimit; i++) {
            assertSame("Element at index " + i + " is the same as element at same index in original", testChoices[i], offsetChoices[i]);
        }

        offsetChoices = authorLookup.doOffset(testChoices, 0, testListInitialSize + 2);
        assertEquals("Too large limit does not alter length", testListInitialSize, offsetChoices.length);
        assertTrue("Too large limit doesn't change the array", Arrays.deepEquals(testChoices, offsetChoices));

        /* start tests * /
        int testStart = 10;
        offsetChoices = authorLookup.doOffset(testChoices, testStart, 0);
        assertEquals("Start appropriately changes number of elements", testListInitialSize - testStart, offsetChoices.length);

        for (int i = testStart; i < testListInitialSize; i++) {
            assertSame("Element at start plus " + i + " is same as element at index " + i + " in original", testChoices[i], offsetChoices[i - testStart]);
        }

        offsetChoices = authorLookup.doOffset(testChoices, testListInitialSize + 1, 0);
        assertNotNull("Too large start value does no harm", offsetChoices);
        assertEquals("Too large start value yields empty result", 0, offsetChoices.length);

        /* start + limit tests * /
        offsetChoices = authorLookup.doOffset(testChoices, testStart, testLimit);
        assertEquals("Start+limit appropriately change number of elements", testLimit, offsetChoices.length);
        for (int i = 0; i < testLimit && i < offsetChoices.length; i++) {
            assertEquals("Start+limit -- offset item matches appropriate original item", testChoices[testStart + i], offsetChoices[i]);
        }

        offsetChoices = authorLookup.doOffset(testChoices, testListInitialSize - 3, 5);
        assertEquals("Too large limit for start leads to right size", 3, offsetChoices.length);
    }
    */
    /* don't run these by default since they rely on the service being up and running*/
    public void testDoQuery() {
	    try {
		    Choice[] choices = authorLookup.doQuery("http://pub.orcid.org/search/orcid-bio/", "Andrea", "Schweer");
		    assertTrue(choices.length > 0);
	    } catch (LookupException e) {
		    e.printStackTrace();
		    assertTrue(false);
	    }
    }
    /*
	public void testDoReverseQuery() {
		try {
			String result = authorLookup.doReverseQuery("http://localhost:8080/unitec-author-lookup/author/reverse_lookup.xml", "6d669816-7327-414c-a58a-c2287a54e712");
			assertEquals("Correct name from reverse lookup", "Papoutsaki, Evangelia", result);
		} catch (LookupException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

    public void testReadChoiceList() {
	    Document doc = readFileAsDoc("authors.xml");
	    assertNotNull(doc);

	    List<Choice> choices = null;
        try {
            choices = authorLookup.readChoiceList(doc);
        } catch (LookupException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        assertNotNull(choices);
        assertEquals("Expected number of choices generated", 1, choices.size());
    }

	public void testReadLabel() {
		Document doc = readFileAsDoc("papoutsaki.xml");
		assertNotNull(doc);
		try {
			String name = authorLookup.readLabel(doc);
			assertEquals("Read correct name from xml doc", "Papoutsaki, Evangelia", name);
		} catch (LookupException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

    public void testCreateChoiceFromAuthor() {
        String id = "756d09d0-e843-459d-af47-446b0a3b1a90";
        String name = "Du Plessis, Andries";
        String phone = "+64-9-815 4321 ext 8923";
        String role = "Senior Lecturer; Programme Leader - MBus; Management and Marketing";
        String label = "Du Plessis, Andries (Senior Lecturer; Programme Leader - MBus; Management and Marketing) phone: +64-9-815 4321 ext 8923";

        Node authorNode = buildAuthorNode(id, name, phone, role);
        
        Choice choice = authorLookup.createChoiceFromAuthor(authorNode);
        assertNotNull(choice);
        assertEquals("IDs match", id, choice.authority);
        assertEquals("Names match", name, choice.value);
        assertEquals("Labels match", label, choice.label);
    }

    private Node buildAuthorNode(String id, String name, String phone, String role) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
	        assertTrue(false);
        }
        Document doc = builder.newDocument();
        Element authorNode = doc.createElement("author");
        doc.appendChild(authorNode);

        Element idNode = doc.createElement("id");
        idNode.setTextContent(id);
        authorNode.appendChild(idNode);

        Element nameNode = doc.createElement("name");
        nameNode.setTextContent(name);
        authorNode.appendChild(nameNode);

        Element roleNode = doc.createElement("role");
        roleNode.setTextContent(role);
        authorNode.appendChild(roleNode);

        Element phoneNode = doc.createElement("phone");
        phoneNode.setTextContent(phone);
        authorNode.appendChild(phoneNode);
        return authorNode;
    }

	private Document readFileAsDoc(String fileName) {
		InputStream stream = ClassLoader.getSystemResourceAsStream(fileName);
		assertNotNull(stream);

		Document doc = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = null;
			builder = factory.newDocumentBuilder();
			doc = builder.parse(stream);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (SAXException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		return doc;
	}
    */
}