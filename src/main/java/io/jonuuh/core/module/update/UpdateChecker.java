package io.jonuuh.core.module.update;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Scanner;

public class UpdateChecker
{
    private static UpdateChecker instance;
    private static String modID;
    private final String latestVersionStr;
    private final boolean isUpdateAvailable;

    public static void createInstance(String modID, String currentVersionStr)
    {
        if (instance != null)
        {
            throw new IllegalStateException("[" + modID + "] UpdateChecker instance has already been created");
        }

        instance = new UpdateChecker(modID, currentVersionStr);
    }

    public static UpdateChecker getInstance()
    {
        if (instance == null)
        {
            throw new NullPointerException("[" + modID + "] UpdateChecker instance has not been created");
        }

        return instance;
    }

    private UpdateChecker(String modID, String currentVersionStr)
    {
        UpdateChecker.modID = modID;

        // TODO:
        String url = "https://raw.githubusercontent.com/jonuuh-mc/UpdateChecker/refs/heads/master/src/main/resources/versions/" + modID + ".txt";
        this.latestVersionStr = parseLatestVersionStr(url);

        Version current = new Version(modID, currentVersionStr);
        Version latest = new Version(modID, latestVersionStr);

        this.isUpdateAvailable = current.compareTo(latest) < 0;
        String sign = isUpdateAvailable ? "<" : ">=";
        System.out.println("[" + modID + "] isUpdateAvailable = " + isUpdateAvailable + "; (current:" + current + ") " + sign + " (latest:" + latest + ")");
    }

    public String getLatestVersionStr()
    {
        return latestVersionStr;
    }

    public boolean isUpdateAvailable()
    {
        return isUpdateAvailable;
    }

    private String parseLatestVersionStr(String url)
    {
        try
        {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(url).openConnection();
            // All of this is only needed because github.io domain certificate is not in java cert store by default? (https://stackoverflow.com/a/34533740)
            httpsURLConnection.setSSLSocketFactory(createSSLContext(getGithubIOCertificateString()).getSocketFactory());

            Scanner scanner = new Scanner(httpsURLConnection.getInputStream());
            String version = scanner.hasNextLine() ? scanner.nextLine() : "";

            scanner.close();
            return version;
        }
        catch (GeneralSecurityException | IOException e)
        {
            System.out.println("[" + modID + "] Failed to access or read version file");
            e.printStackTrace();
            return "";
        }
    }

    private SSLContext createSSLContext(String derCertificateString) throws GeneralSecurityException, IOException
    {
        ByteArrayInputStream derInputStream = new ByteArrayInputStream(derCertificateString.getBytes());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) cf.generateCertificate(derInputStream);
        String alias = certificate.getSubjectX500Principal().getName();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        trustStore.setCertificateEntry(alias, certificate);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(trustStore, null);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }

    private String getGithubIOCertificateString()
    {
        InputStream inputStream = UpdateChecker.class.getClassLoader().getResourceAsStream("assets/_.github.io.crt");

        if (inputStream != null)
        {
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String certificateStr = scanner.next();
            scanner.close();
            return certificateStr;
        }
        return "";
    }
}

