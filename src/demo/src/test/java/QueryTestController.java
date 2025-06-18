import com.oscaruiz.mycqrs.core.domain.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.query.GetBookByTitleQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueryTestController {

    private final QueryBus queryBus;

    public QueryTestController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/query/book")
    public String testQuery(@RequestParam String title) {
        return queryBus.handle(new GetBookByTitleQuery(title));
    }
}
