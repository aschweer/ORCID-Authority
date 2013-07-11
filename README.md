ORCID-Authority
===============

Developer Challenge entry for Open Repositories 2013
By Andrea Schweer and Ryan Scherle

Integrate ORCID with DSpace through providing person lookup functionality in
the DSpace submission interface and through backing author name metadata with
ORCID IDs.

Demonstrator implementation for the second use case in Ryan's ORCID/DSpace
proposal: https://wiki.duraspace.org/display/DSPACE/ORCID+Integration

Run mvn package install to install jar file locally. See dspace-changes file
for the changes you need to make to your DSpace (3.1) to pull in the lookup
functionality.

Known limitations:
- no tests / existing tests don't work
- no reverse lookup (orcid id -> name)
- not much help in disambiguating multiple hits
- not much flexibility around missing values / spaces or non-ASCII characters in name
