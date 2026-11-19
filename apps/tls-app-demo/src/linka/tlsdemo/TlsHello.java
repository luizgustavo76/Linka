package linka.tlsdemo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Vector;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.NamedGroup;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsClientProtocol;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

public final class TlsHello {
    private final URL endpoint;
    private final byte[] fingerprint;
    private final SecureRandom random = new SecureRandom();

    public TlsHello(String endpoint, String fingerprint) throws IOException {
        this.endpoint = new URL(endpoint);
        if (!"https".equals(this.endpoint.getProtocol()) || this.endpoint.getHost().length() == 0
                || this.endpoint.getUserInfo() != null || this.endpoint.getRef() != null
                || !this.endpoint.toExternalForm().matches("[!-~]+")) {
            throw new IOException("Enter an HTTPS URL with no credentials or fragment.");
        }
        String hex = fingerprint.replace(":", "").trim();
        if (!hex.matches("[0-9a-fA-F]{64}")) {
            throw new IOException("Enter the server certificate SHA-256 fingerprint.");
        }
        this.fingerprint = Hex.decode(hex);
    }

    public String get() throws IOException {
        Socket socket = new Socket();
        try {
            String host = endpoint.getHost();
            int port = endpoint.getPort() == -1 ? 443 : endpoint.getPort();
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(5000);
            TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(),
                    socket.getOutputStream());
            protocol.connect(new DefaultTlsClient(new BcTlsCrypto(random)) {
                protected ProtocolVersion[] getSupportedVersions() {
                    return ProtocolVersion.TLSv13.only();
                }

                protected int[] getSupportedCipherSuites() {
                    return new int[] { CipherSuite.TLS_CHACHA20_POLY1305_SHA256 };
                }

                @SuppressWarnings("rawtypes")
                protected Vector getSupportedGroups(Vector roles) {
                    Vector<Integer> groups = new Vector<Integer>();
                    groups.addElement(Integer.valueOf(NamedGroup.x25519));
                    return groups;
                }

                public TlsAuthentication getAuthentication() {
                    return new TlsAuthentication() {
                        public void notifyServerCertificate(TlsServerCertificate server)
                                throws IOException {
                            if (server.getCertificate().isEmpty()) {
                                throw new IOException("Server sent no certificate.");
                            }
                            byte[] der = server.getCertificate().getCertificateAt(0).getEncoded();
                            SHA256Digest digest = new SHA256Digest();
                            byte[] actual = new byte[digest.getDigestSize()];
                            digest.update(der, 0, der.length);
                            digest.doFinal(actual, 0);
                            if (!Arrays.constantTimeAreEqual(fingerprint, actual)) {
                                throw new IOException("Server certificate fingerprint mismatch.");
                            }
                        }

                        public TlsCredentials getClientCredentials(CertificateRequest request) {
                            return null;
                        }
                    };
                }
            });

            try {
                String path = endpoint.getFile();
                if (path.length() == 0 || path.charAt(0) == '?') {
                    path = "/" + path;
                }
                OutputStream out = protocol.getOutputStream();
                out.write(("GET " + path + " HTTP/1.1\r\nHost: " + endpoint.getAuthority()
                        + "\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
                out.flush();
                return readHello(protocol.getInputStream());
            } finally {
                protocol.close();
            }
        } finally {
            socket.close();
        }
    }

    private static String readHello(InputStream stream) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(stream));
        String status = readLine(in);
        if (!status.startsWith("HTTP/1.1 200 ")) {
            throw new IOException("Unexpected HTTP status: " + status);
        }
        int length = -1;
        for (String header = readLine(in); header.length() != 0; header = readLine(in)) {
            if (header.toLowerCase(Locale.US).startsWith("content-length:")) {
                try {
                    length = Integer.parseInt(header.substring(15).trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid Content-Length.");
                }
            }
        }
        if (length < 0 || length > 4096) {
            throw new IOException("Expected a Content-Length between 0 and 4096.");
        }
        byte[] body = new byte[length];
        in.readFully(body);
        return new String(body, "UTF-8");
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        for (int value = in.read(); value != -1 && line.size() < 8192; value = in.read()) {
            if (value == '\r' && in.read() == '\n') {
                return line.toString("US-ASCII");
            }
            line.write(value);
        }
        throw new IOException("Incomplete or oversized HTTP header line.");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !("1".equals(args[2]) || "50".equals(args[2]))) {
            throw new IllegalArgumentException("Usage: TlsHello HTTPS_URL CERT_SHA256 1|50");
        }
        TlsHello client = new TlsHello(args[0], args[1]);
        int count = Integer.parseInt(args[2]);
        for (int i = 1; i <= count; i++) {
            System.out.println(i + "/" + count + " " + client.get());
        }
    }
}
