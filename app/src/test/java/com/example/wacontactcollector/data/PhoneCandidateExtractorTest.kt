package com.example.wacontactcollector.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneCandidateExtractorTest {
    @Test fun extractsOnlyAValidPhonePattern() {
        assertEquals("6281234567890", PhoneCandidateExtractor.find("Contact: +62 812-3456-7890"))
    }

    @Test fun doesNotTreatMessageTextAsACandidate() {
        assertNull(PhoneCandidateExtractor.find("Halo, sampai nanti ya"))
    }
}
