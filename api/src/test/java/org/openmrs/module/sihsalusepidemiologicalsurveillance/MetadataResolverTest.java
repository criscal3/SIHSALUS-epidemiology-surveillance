package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.Metadata;

public class MetadataResolverTest {
	
	private SyntheticFixture configured() {
		SyntheticFixture f = new SyntheticFixture();
		int id = 120;
		for (String key : Arrays.asList("IMPORTED_NATIONAL", "IMPORTED_INTERNATIONAL", "INDUCED", "INTRODUCED", "RELAPSE",
		    "RECRUDESCENCE")) {
			Metadata.Choice choice = new Metadata.Choice();
			choice.key = key;
			choice.label = key;
			Concept answer = SyntheticFixture.concept(id++);
			choice.conceptUuid = answer.getUuid();
			when(f.clinical.concept(answer.getUuid())).thenReturn(answer);
			f.m.origins.add(choice);
		}
		for (String key : f.m.questions.keySet()) {
			Concept question = f.clinical.concept(f.m.questions.get(key));
			ConceptDatatype datatype = new ConceptDatatype();
			datatype.setName(Arrays.asList("event", "status", "severity", "origin", "species").contains(key) ? "Coded"
			        : "onset".equals(key) ? "Date" : "pregnancy".equals(key) ? "Boolean" : "Text");
			question.setDatatype(datatype);
		}
		answers(f, "status", f.m.statuses);
		answers(f, "origin", f.m.origins);
		answers(f, "severity", f.disease.severities);
		when(f.clinical.concept(f.event.getConcept().getUuid())).thenReturn(f.event.getConcept());
		f.clinical.concept(f.m.questions.get("event")).addAnswer(new ConceptAnswer(f.event.getConcept()));
		when(f.clinical.conceptSource(f.m.icd10SourceUuid)).thenReturn(new ConceptSource());
		return f;
	}
	
	private void answers(SyntheticFixture f, String key, List<Metadata.Choice> choices) {
		for (Metadata.Choice choice : choices)
			f.clinical.concept(f.m.questions.get(key)).addAnswer(new ConceptAnswer(f.clinical.concept(choice.conceptUuid)));
	}
	
	private MetadataResolver resolver(SyntheticFixture f) throws Exception {
		when(f.clinical.property(SurveillanceConstants.MODULE_ID + ".metadata"))
		        .thenReturn(new ObjectMapper().writeValueAsString(f.m));
		MetadataResolver resolver = new MetadataResolver();
		resolver.setClinical(f.clinical);
		resolver.setDao(f.dao);
		return resolver;
	}
	
	@Test
	public void validatesExistingQuestionsAnswersAndMappings() throws Exception {
		SyntheticFixture f = configured();
		assertEquals(f.m.questions, resolver(f).get().questions);
	}
	
	@Test
	public void refusesUnconfiguredClinicalOperations() throws Exception {
		SyntheticFixture f = configured();
		MetadataResolver resolver = resolver(f);
		when(f.clinical.property(anyString())).thenReturn("");
		rejected(resolver);
	}
	
	@Test
	public void refusesWrongQuestionDatatype() throws Exception {
		SyntheticFixture f = configured();
		f.clinical.concept(f.m.questions.get("onset")).getDatatype().setName("Text");
		rejected(resolver(f));
	}
	
	@Test
	public void refusesAnswerOutsideExistingQuestion() throws Exception {
		SyntheticFixture f = configured();
		f.clinical.concept(f.m.questions.get("status")).setAnswers(new HashSet<ConceptAnswer>());
		rejected(resolver(f));
	}
	
	@Test
	public void refusesUnverifiedPregnancyAttribute() throws Exception {
		SyntheticFixture f = configured();
		f.m.pregnancyAttributeTypeUuid = SyntheticFixture.uuid(200);
		PersonAttributeType type = new PersonAttributeType();
		type.setFormat("java.lang.String");
		when(f.clinical.attributeType(f.m.pregnancyAttributeTypeUuid)).thenReturn(type);
		rejected(resolver(f));
	}
	
	@Test
	public void refusesSilentReplicaFallback() throws Exception {
		SyntheticFixture f = configured();
		f.m.analyticsDatasource = "replica";
		rejected(resolver(f));
	}
	
	private void rejected(MetadataResolver resolver) {
		try {
			resolver.get();
			fail("Expected metadata rejection");
		}
		catch (SurveillanceException expected) {
			assertEquals(503, expected.getStatus());
			assertEquals("METADATA_NOT_CONFIGURED", expected.getCode());
		}
	}
}
