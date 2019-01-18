package org.ofbiz.core.entity.external;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.ofbiz.core.entity.external.ExternalDataHandler.ExternalData;
import org.w3c.dom.Element;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class TestExternalDataHandler {

    private ExternalDataHandler externalDataHandler;

    @Before
    public void setup() {
        externalDataHandler = new ExternalDataHandler();
    }

    @Test
    public void shouldNotFailOnNullArgs() {
        Optional<String> data = externalDataHandler.getExternalData(null, null);
        assertFalse(data.isPresent());
        data = externalDataHandler.getExternalData(null, ExternalData.PASSWORD);
        assertFalse(data.isPresent());
        data = externalDataHandler.getExternalData(mock(Element.class), null);
        assertFalse(data.isPresent());
    }

    @Test
    public void shouldNotFailOnWrongClassFile() {
        Element data = mock(Element.class);
        when(data.getAttribute(ExternalData.PASSWORD.key)).thenReturn("com.example.test.not.existing.class.Test.class");

        Optional<String> password = externalDataHandler.getExternalData(data, ExternalData.PASSWORD);

        assertFalse(password.isPresent());
    }

    @Test
    public void readDataFromExternalProvider() {
        Element data = mock(Element.class);
        when(data.getAttribute(ExternalData.PASSWORD.key)).thenReturn("org.ofbiz.core.entity.external.MockExternalDataProvider");

        Optional<String> password = externalDataHandler.getExternalData(data, ExternalData.PASSWORD);

        assertTrue(password.isPresent());
        assertEquals("mock", password.get());
    }
}