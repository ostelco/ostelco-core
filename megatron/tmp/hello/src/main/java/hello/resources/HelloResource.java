package hello.resources;

import com.google.common.base.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class HelloResource {
    private final String template;

    public HelloResource(String template) {
        this.template = template;
    }

    @GET
    public Map<String, Object> sayHello(@QueryParam("name") Optional<String> name) {
        Map<String, Object> result = new HashMap<>();
        result.put("hello", String.format(template, name.or("stranger")));
        return result;
    }
}

