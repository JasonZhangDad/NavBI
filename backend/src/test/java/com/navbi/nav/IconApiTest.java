package com.navbi.nav;

import com.navbi.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IconApiTest extends ApiTestBase {

    @Test
    void unknownNavIdReturns404() throws Exception {
        mvc.perform(get("/api/nav/icon/99999")).andExpect(status().isNotFound());
    }

    @Test
    void iconEndpointIsPublic() throws Exception {
        // 无 token 访问不应 401（未知 id 返回 404 而非未认证）
        mvc.perform(get("/api/nav/icon/88888")).andExpect(status().isNotFound());
    }
}
