package golf.skc.web.rest.util

import org.junit.Assert.*
import org.junit.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import java.util.*

/**
 * Tests based on parsing algorithm in app/components/util/pagination-util.service.js
 *
 * @see PaginationUtil
 */
class PaginationUtilUnitTest {

  @Test
  fun generatePaginationHttpHeadersTest() {
    val baseUrl = "/api/_search/example"
    val content = ArrayList<String>()
    val page = PageImpl(content, PageRequest.of(6, 50), 400L)
    val headers = PaginationUtil.generatePaginationHttpHeaders(page, baseUrl)
    val strHeaders = headers[HttpHeaders.LINK]
    assertNotNull(strHeaders)
    assertTrue(strHeaders!!.size == 1)
    val headerData = strHeaders[0]
    assertTrue(headerData.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size == 4)
    val expectedData = ("</api/_search/example?page=7&size=50>; rel=\"next\","
      + "</api/_search/example?page=5&size=50>; rel=\"prev\","
      + "</api/_search/example?page=7&size=50>; rel=\"last\","
      + "</api/_search/example?page=0&size=50>; rel=\"first\"")
    assertEquals(expectedData, headerData)
    val xTotalCountHeaders = headers["X-Total-Count"]
    assertTrue(xTotalCountHeaders!!.size == 1)
    assertTrue(java.lang.Long.valueOf(xTotalCountHeaders[0]) == 400L)
  }

}
