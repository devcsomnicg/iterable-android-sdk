package com.iterable.iterableapi;

import android.net.Uri;
import android.util.Base64;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.iterable.iterableapi.IterableConstants.HEADER_SDK_AUTH_FORMAT;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class IterableAuthManagerTest extends BaseTest {

    private IterableUtil.IterableUtilImpl originalIterableUtil;
    private IterableUtil.IterableUtilImpl iterableUtilSpy;
    private MockWebServer server;
    private IterableAuthHandler authHandler;

    private String validJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE2MTYyMzkwMjJ9.GOKZqDEYCIuuuWAgOXLiSE9FZafJ0vV9SY9DaWTAb3g";
    private String expiredJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Before
    public void setUp() {

        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        reInitIterableApi();

        originalIterableUtil = IterableUtil.instance;
        iterableUtilSpy = spy(originalIterableUtil);
        IterableUtil.instance = iterableUtilSpy;

        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setAuthHandler(authHandler);
            }
        });
    }

    @After
    public void tearDown() throws IOException {
        IterableUtil.instance = originalIterableUtil;
        iterableUtilSpy = null;
        server.shutdown();
        server = null;
    }

    private void reInitIterableApi() {
        IterableApi.sharedInstance = spy(new IterableApi());
//        IterableAuthManager authManagerMock = mock(IterableAuthManager.class);
//        doReturn(authManagerMock).when(IterableApi.sharedInstance).getAuthManager();
        authHandler = mock(IterableAuthHandler.class);
    }

    @Test
    public void testSetEmailWithToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();

        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), validJWT);
    }

    @Test
    public void testSetEmailWithTokenExpired() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        doReturn(expiredJWT).when(authHandler).onAuthTokenRequested();

        IterableApi.getInstance().setEmail(email);
        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), expiredJWT);
    }

    @Test
    public void testSetUserIdWithToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";
        doReturn(validJWT).when(authHandler).onAuthTokenRequested();
        IterableApi.getInstance().setUserId(userId);

        assertEquals(userId, IterableApi.getInstance().getUserId());
        assertEquals(validJWT, IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSameEmailWithNewToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        String token = "token";

//        IterableApi.getInstance().setEmail(email, token);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        String newToken = "asdf";

//        IterableApi.getInstance().setEmail(email, newToken);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), newToken);
    }

    @Test
    public void testSameUserIdWithNewToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";
        String token = "token";

        IterableApi.getInstance().setUserId(userId, token);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        String newToken = "asdf";

        IterableApi.getInstance().setUserId(userId, newToken);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), newToken);
    }

    @Test
    public void testSetSameEmailAndRemoveToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        String token = "token";

//        IterableApi.getInstance().setEmail(email, token);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameUserIdAndRemoveToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";
        String token = "token";

        IterableApi.getInstance().setUserId(userId, token);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameEmail() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";

        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());

        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameUserId() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";

        IterableApi.getInstance().setUserId(userId);

        assertNull(IterableApi.getInstance().getEmail());
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertNull(IterableApi.getInstance().getAuthToken());

        IterableApi.getInstance().setUserId(userId);

        assertNull(IterableApi.getInstance().getEmail());
        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testSetSameEmailWithSameToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";
        String token = "token";

//        IterableApi.getInstance().setEmail(email, token);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

//        IterableApi.getInstance().setEmail(email, token);

        assertEquals(IterableApi.getInstance().getEmail(), email);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);
    }

    @Test
    public void testSetSameUserIdWithSameToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";
        String token = "token";

        IterableApi.getInstance().setUserId(userId, token);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);

        IterableApi.getInstance().setUserId(userId, token);

        assertEquals(IterableApi.getInstance().getUserId(), userId);
        assertEquals(IterableApi.getInstance().getAuthToken(), token);
    }

    @Test
    public void testEmailLogOut() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String email = "test@example.com";

        IterableApi.getInstance().setEmail(email);

        assertEquals(IterableApi.getInstance().getEmail(), email);

        IterableApi.getInstance().setEmail(null);

        assertNull(IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testUserIdLogOut() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        String userId = "testUserId";

        IterableApi.getInstance().setUserId(userId);

        assertEquals(IterableApi.getInstance().getUserId(), userId);

        IterableApi.getInstance().setUserId(null);

        assertNull(IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getAuthToken());
    }

    @Test
    public void testAuthTokenPresentInRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        String token = "token";
//        IterableApi.getInstance().setEmail("new@email.com", token);
        IterableApi.getInstance().updateEmail("newEmail@gmail.com", null, null);
        RecordedRequest updateEmailRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(updateEmailRequest);

        assertEquals(HEADER_SDK_AUTH_FORMAT + token, updateEmailRequest.getHeader("Authorization"));

        IterableApi.getInstance().setEmail("new@email.com");
        IterableApi.getInstance().updateEmail("new@email2.com");
        updateEmailRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(updateEmailRequest);
        assertNull(updateEmailRequest.getHeader("Authorization"));
    }
}
