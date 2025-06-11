
var mockData = [ {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9",
		"@type" : [ "http://www.w3.org/2002/07/owl#Ontology" ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Amount",
		"@type" : [ "http://www.w3.org/2002/07/owl#DatatypeProperty" ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Cognizant",
		"@type" : [ "http://www.w3.org/2002/07/owl#NamedIndividual", "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Sponsors" ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Events",
		"@type" : [ "http://www.w3.org/2002/07/owl#Class" ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Google",
		"@type" : [ "http://www.w3.org/2002/07/owl#NamedIndividual", "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Sponsors" ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Hackathon",
		"@type" : [ "http://www.w3.org/2002/07/owl#NamedIndividual", "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Tech" ],
		"http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#isSponsoredBy" : [ {
			"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Google"
		} ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Robotics",
		"@type" : [ "http://www.w3.org/2002/07/owl#NamedIndividual", "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Tech" ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Sponsors",
		"@type" : [ "http://www.w3.org/2002/07/owl#Class" ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Tech",
		"@type" : [ "http://www.w3.org/2002/07/owl#Class" ],
		"http://www.w3.org/2000/01/rdf-schema#subClassOf" : [ {
			"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Events"
		} ]
		}, {
		"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#isSponsoredBy",
		"@type" : [ "http://www.w3.org/2002/07/owl#ObjectProperty" ],
		"http://www.w3.org/2000/01/rdf-schema#domain" : [ {
			"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Tech"
		} ],
		"http://www.w3.org/2000/01/rdf-schema#range" : [ {
			"@id" : "http://www.semanticweb.org/583154/ontologies/2018/6/untitled-ontology-9#Sponsors"
		} ]
} ]

d3.jsonldVis(mockData, '#graph', {
  w: 800,
  h: 600,
  maxLabelWidth: 250,
  tipClassName: 'tip-class'
});