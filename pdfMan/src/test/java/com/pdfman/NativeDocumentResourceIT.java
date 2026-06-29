package com.pdfman;

import io.quarkus.test.junit.QuarkusIntegrationTest;

// Feature: pdf-man-service — re-runs all DocumentResourceTest cases against the native binary
@QuarkusIntegrationTest
public class NativeDocumentResourceIT extends DocumentResourceTest {
    // Empty body: all test methods are inherited from DocumentResourceTest
}
