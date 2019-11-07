package org.ostelco.prime.ekyc.myinfo.v3

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.ostelco.ext.myinfo.JsonUtils

class ModelTest {

    @Test
    fun `test - Model class`() {
        val personData = jacksonObjectMapper()
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(personDataString, PersonData::class.java)

        assertNotNull(personData)

        assertEquals("1998-06-06", personData.dateOfBirth?.value)
        assertEquals("2040-06-06", personData.passExpiryDate?.value)
    }
}

private val personDataString = JsonUtils.compactJson("""
{
  "name": {
    "lastupdated": "2019-04-05",
    "source": "1",
    "classification": "C",
    "value": "TAN XIAO HUI"
  },
  "sex": {
    "lastupdated": "2019-04-05",
    "code": "F",
    "source": "1",
    "classification": "C",
    "desc": "FEMALE"
  },
  "nationality": {
    "lastupdated": "2019-04-05",
    "code": "SG",
    "source": "1",
    "classification": "C",
    "desc": "SINGAPORE CITIZEN"
  },
  "dob": {
    "lastupdated": "2019-04-05",
    "source": "1",
    "classification": "C",
    "value": "1998-06-06"
  },
  "email": {
    "lastupdated": "2019-04-05",
    "source": "2",
    "classification": "C",
    "value": "myinfotesting@gmail.com"
  },
  "mobileno": {
    "lastupdated": "2019-04-05",
    "source": "2",
    "classification": "C",
    "areacode": {
      "value": "65"
    },
    "prefix": {
      "value": "+"
    },
    "nbr": {
      "value": "97399245"
    }
  },
  "regadd": {
    "country": {
      "code": "SG",
      "desc": "SINGAPORE"
    },
    "unit": {
      "value": "128"
    },
    "street": {
      "value": "BEDOK NORTH AVENUE 4"
    },
    "lastupdated": "2019-04-05",
    "block": {
      "value": "102"
    },
    "source": "1",
    "postal": {
      "value": "460102"
    },
    "classification": "C",
    "floor": {
      "value": "09"
    },
    "type": "SG",
    "building": {
      "value": "PEARL GARDEN"
    }
  },
  "mailadd": {
    "country": {
      "code": "SG",
      "desc": "SINGAPORE"
    },
    "unit": {
      "value": "128"
    },
    "street": {
      "value": "BEDOK NORTH AVENUE 4"
    },
    "lastupdated": "2019-04-05",
    "block": {
      "value": "102"
    },
    "source": "1",
    "postal": {
      "value": "460102"
    },
    "classification": "C",
    "floor": {
      "value": "09"
    },
    "type": "SG",
    "building": {
      "value": "PEARL GARDEN"
    }
  },
  "passexpirydate": {
    "lastupdated": "2019-04-05",
    "source": "1",
    "classification": "C",
    "value": "2040-06-06"
  },
  "uinfin": {
    "value": "S1111111D",
    "classification": "C",
    "source": "1",
    "lastupdated": "2019-03-26"
  }
}
    """)