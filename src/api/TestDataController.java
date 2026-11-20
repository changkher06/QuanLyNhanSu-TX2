package api;

import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Opt-in test utility. Disabled by default; never expose this on an untrusted network. */
@RestController
@ConditionalOnProperty(name = "tx2.test-data.enabled", havingValue = "true")
public class TestDataController {
    private final SampleData data;
    public TestDataController(SampleData data) { this.data = data; }
    @PostMapping("/api/test-data/reset")
    public ApiDtos.ResetResult reset() throws IOException { return data.reset(); }
}
