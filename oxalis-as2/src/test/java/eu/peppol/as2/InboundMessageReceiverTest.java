package eu.peppol.as2;

import eu.peppol.security.KeystoreManager;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * Simulates reception of a an AS2 Message, which is validated etc. and finally produces a MDN.
 *
 * @author steinar
 *         Date: 21.10.13
 *         Time: 21:50
 */
public class InboundMessageReceiverTest {

    private ByteArrayInputStream inputStream;
    private HashMap<String,String> headers;

    @BeforeMethod
    public void createHeaders() {
        headers = new HashMap<String, String>();
        headers.put(As2Header.DISPOSITION_NOTIFICATION_OPTIONS.getHttpHeaderName(), "Disposition-Notification-Options: signed-receipt-protocol=required, pkcs7-signature; signed-receipt-micalg=required,sha1");
        headers.put(As2Header.AS2_TO.getHttpHeaderName(), "APP_1000000006");
        headers.put(As2Header.AS2_FROM.getHttpHeaderName(), "APP_1000000006");
        headers.put(As2Header.MESSAGE_ID.getHttpHeaderName(), "42");
        headers.put(As2Header.AS2_VERSION.getHttpHeaderName(), As2Header.VERSION);
        headers.put(As2Header.SUBJECT.getHttpHeaderName(), "An AS2 message");
        headers.put(As2Header.DATE.getHttpHeaderName(), "Mon Oct 21 22:01:48 CEST 2013");
    }


    @BeforeMethod
    public void createInputStream() throws MimeTypeParseException, IOException, MessagingException {
        MimeMessageFactory mimeMessageFactory = new MimeMessageFactory(KeystoreManager.getInstance().getOurPrivateKey(), KeystoreManager.getInstance().getOurCertificate());

        // Fetch input stream for data
        InputStream resourceAsStream = MimeMessageFactory.class.getClassLoader().getResourceAsStream("example.xml");
        assertNotNull(resourceAsStream);

        // Creates the signed message
        MimeMessage signedMimeMessage = mimeMessageFactory.createSignedMimeMessage(resourceAsStream, new MimeType("application","xml"));
        assertNotNull(signedMimeMessage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        signedMimeMessage.writeTo(baos);

        inputStream = new ByteArrayInputStream(baos.toByteArray());
    }

    @Test
    public void loadAndReceiveTestMessageOK() throws Exception {

        InboundMessageReceiver inboundMessageReceiver = new InboundMessageReceiver();
        MdnData mdnData = inboundMessageReceiver.receive(headers, inputStream);

        assertEquals(mdnData.getAs2Disposition().getDispositionType(), As2Disposition.DispositionType.PROCESSED);
        AssertJUnit.assertNotNull(mdnData.getMic());
    }

    /**
     * Specifies an invalid MIC algorithm (MD5), which should cause reception to fail.
     *
     * @throws Exception
     */
    @Test
    public void receiveMessageWithInvalidDispositionRequest() throws Exception {

        headers.put(As2Header.DISPOSITION_NOTIFICATION_OPTIONS.getHttpHeaderName(), "Disposition-Notification-Options: signed-receipt-protocol=required, pkcs7-signature; signed-receipt-micalg=required,md5");

        InboundMessageReceiver inboundMessageReceiver = new InboundMessageReceiver();

        MdnData mdnData = null;
        try {
            mdnData = inboundMessageReceiver.receive(headers, inputStream);
            fail("Reception of AS2 messages request MD5 as the MIC algorithm, should have failed");
        } catch (ErrorWithMdnException e) {
            assertNotNull(e.getMdnData(), "MDN should have been returned upon reception of invalid AS2 Message");
            assertEquals(e.getMdnData().getAs2Disposition().getDispositionType(), As2Disposition.DispositionType.FAILED);
        }
    }
}