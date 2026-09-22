package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db.ClinicalData;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.ClinicalCatalog;

public class ClinicalCatalogServiceTest {
	
	@Test
	public void opensFixedCatalogWithoutRequiringInstalledConceptsOrEvents() {
		ClinicalData clinical = mock(ClinicalData.class);
		ClinicalCatalogService service = new ClinicalCatalogService();
		service.setClinical(clinical);
		ClinicalCatalog catalog = service.get();
		assertEquals(2, catalog.diseases.size());
		assertNull(catalog.trueConceptUuid);
		verify(clinical, never()).concept(anyString());
	}
	
	@Test
	public void eachRequestReceivesAnIndependentFixedCatalog() {
		ClinicalCatalog first = SurveillanceCatalog.create();
		first.diseases.clear();
		assertEquals(2, SurveillanceCatalog.create().diseases.size());
	}
}
