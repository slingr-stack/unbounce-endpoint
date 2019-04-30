package io.slingr.endpoints.unbounce;

import io.slingr.endpoints.services.rest.RestMethod;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.tests.EndpointTests;
import io.slingr.endpoints.ws.exchange.WebServiceResponse;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 *
 * Created by dgaviola on 20/05/15.
 */
@Ignore("For dev proposes")
public class UnbounceEndpointTest {
    private static final Logger logger = LoggerFactory.getLogger(UnbounceEndpointTest.class);

    private static EndpointTests test;

    @BeforeClass
    public static void init() throws Exception {
        test = EndpointTests.start(new io.slingr.endpoints.unbounce.Runner(), "test.properties");
    }

    @Test
    public void testHttpQueries() {
        try {
            invalidToken();
            validHttpQuery();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to execute test.");
        }

        logger.info("-- END");
    }

    private void invalidToken() {
        test.clearReceivedEvents();
        assertTrue(test.getReceivedEvents().isEmpty());

        final Json dataJson = Json.map().set("data.json", "{\"name\":[\"test\"],\"email\":[\"dasda@sfsd.com\"],\"token\":[\"abc\"],\"ip_address\":[\"198.41.231.85\"],\"time_submitted\":[\"03:19 AM UTC\"]}");
        final WebServiceResponse response = test.executeWebServices(RestMethod.POST, "", dataJson, true);
        assertNotNull(response);
    }

    private void validHttpQuery() {
        test.clearReceivedEvents();
        assertTrue(test.getReceivedEvents().isEmpty());

        final Json dataJson = Json.map().set("data.json", "{\"name\":[\"test\"],\"email\":[\"dasda@sfsd.com\"],\"token\":[\"123\"],\"ip_address\":[\"198.41.231.85\"],\"time_submitted\":[\"03:19 AM UTC\"]}");
        final WebServiceResponse response = test.executeWebServices(RestMethod.POST, "", dataJson);
        assertNotNull(response);

        assertFalse(test.getReceivedEvents().isEmpty());

        Json event = test.getReceivedEvents().get(0);
        assertNotNull(event);
        event = event.json("data");
        assertNotNull(event);
        assertNotNull(event.string("name"));
        assertEquals("test", event.string("name"));
        assertNotNull(event.string("email"));
        assertEquals("dasda@sfsd.com", event.string("email"));
        assertNotNull(event.string("ip_address"));
        assertEquals("198.41.231.85", event.string("ip_address"));
        assertNotNull(event.string("time_submitted"));
        assertEquals("03:19 AM UTC", event.string("time_submitted"));
        assertNull(event.objects("token"));

        test.clearReceivedEvents();
        assertTrue(test.getReceivedEvents().isEmpty());
    }
}
