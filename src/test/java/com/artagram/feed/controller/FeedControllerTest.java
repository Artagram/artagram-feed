package com.artagram.feed.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.service.PublicacionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FeedControllerTest {

    @Test
    void laRutaQueUsaLaPaginaDelFeedPagina() throws Exception {
        JwtUserResolver resolver = mock(JwtUserResolver.class);
        when(resolver.resolverOpcional(any())).thenReturn(Optional.empty());
        PublicacionService service = mock(PublicacionService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new FeedController(service, resolver)).build();

        mvc.perform(get("/api/feed").param("pagina", "2")).andExpect(status().isOk());

        verify(service).feedReciente(null, 2, 10);
    }
}
