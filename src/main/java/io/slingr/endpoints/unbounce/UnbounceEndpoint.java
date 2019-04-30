package io.slingr.endpoints.unbounce;

import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.framework.annotations.EndpointProperty;
import io.slingr.endpoints.framework.annotations.EndpointWebService;
import io.slingr.endpoints.framework.annotations.SlingrEndpoint;
import io.slingr.endpoints.services.HttpService;
import io.slingr.endpoints.services.rest.RestMethod;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.Strings;
import io.slingr.endpoints.ws.exchange.WebServiceRequest;
import io.slingr.endpoints.ws.exchange.WebServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Unbounce endpoint
 *
 * <p>Created by dgaviola on 20/05/15.
 */
@SlingrEndpoint(name = "unbounce")
public class UnbounceEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(UnbounceEndpoint.class);

    @EndpointProperty
    private String validToken;

    @EndpointWebService(methods = RestMethod.POST)
    public WebServiceResponse webhooks(WebServiceRequest request){
        logger.info("Form received from Unbounce");

        final Json body = request.getJsonBody();
        Json data = body.json("data.json");
        if (data == null) {
            throw EndpointException.permanent(ErrorCode.CONVERSION, "No field data.json found");
        }

        String token = null;
        final Json processedJson = Json.map();
        try {
            for (String key : data.keys()) {
                if ("token".equals(key)) {
                    // this is the token we need to validate before processing this form
                    final Object oToken = unwrapSingleValue(data.object(key));
                    if(oToken != null){
                        token = oToken.toString();
                    }
                } else {
                    processedJson.set(key, unwrapSingleValue(data.object(key)));
                }
            }
        } catch (Exception e) {
            Json additionalData = Json.map()
                    .set("formData", data)
                    .set("cause", (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName());
            throw EndpointException.permanent(ErrorCode.CONVERSION, "Error transforming form data", additionalData, e);
        }

        logger.info(String.format("Data received: %s", processedJson.toString()));

        if (token != null && token.equals(validToken)) {
            logger.info("Token is valid");

            events().send("inboundFormSubmitted", processedJson);

            logger.info("Form sent to application");
        } else {
            logger.info("Invalid token");
            throw EndpointException.permanent(ErrorCode.API, "Invalid token");
        }
        return HttpService.defaultWebhookResponse();
    }

    private Object unwrapSingleValue(Object value) {
        // Unbounce sends everything as arrays so we will put things as arrays only when there are more than one value
        if (value instanceof Json && ((Json) value).isList()) {
            Json json = (Json) value;
            if (json.size() == 1) {
                return json.object(0);
            } else if (json.size() == 0) {
                return null;
            }
        }
        return value;
    }

    private Json parseEncodedForm(String form){
        final Json response = Json.map();
        String[] pairs = form.split("&");
        for (String pair : pairs) {
            String[] fields = pair.split("=");
            String name = Strings.urlDecode(fields[0]);
            String value = Strings.urlDecode(fields[1]);
            response.set(name, value);
        }
        return response;
    }
}
