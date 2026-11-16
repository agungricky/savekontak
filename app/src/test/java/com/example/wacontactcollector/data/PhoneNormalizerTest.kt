package com.example.wacontactcollector.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNormalizerTest {
    @Test fun normalizesLocalZero() = assertEquals("6281234567890", PhoneNormalizer.normalize("0812 3456 7890"))
    @Test fun normalizesLocalEight() = assertEquals("6281234567890", PhoneNormalizer.normalize("81234567890"))
    @Test fun keepsInternationalForms() = assertEquals("6281234567890", PhoneNormalizer.normalize("+62 812-3456-7890"))
    @Test fun rejectsUnsupportedNumber() = assertNull(PhoneNormalizer.normalize("021123456"))
}
