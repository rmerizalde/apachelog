This is a port from http://code.google.com/p/apachelog/

The original code is actually from Peter Hickman's Apache::LogRegex Perl module

Includes some utility base classes to index logs into Solr. This is not meant to be produciton ready, in fact the sample schema provided is not optimized and can take 3x space or more of the original logs size. It can be useful for triaging issues (e.g. 404s)
