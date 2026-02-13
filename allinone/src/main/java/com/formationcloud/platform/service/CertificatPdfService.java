package com.formationcloud.platform.service;

import com.formationcloud.platform.model.Certificat;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.Utilisateur;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificatPdfService {

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String buildVerifyUrl(String certificatCode) {
        String base = publicBaseUrl == null || publicBaseUrl.isBlank() ? "http://localhost:8080" : publicBaseUrl;
        // React SPA lives at /app/#/...
        return base + "/app/#/verify/" + certificatCode;
    }

    public byte[] renderCertificatPdfModeleA(Certificat certificat) {
        try {
            String html = renderHtml(certificat);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération PDF certificat {}", certificat.getNumeroUnique(), e);
            throw new RuntimeException("Erreur génération PDF");
        }
    }

    private String renderHtml(Certificat cert) throws IOException, WriterException {
        ClassPathResource tpl = new ClassPathResource("certificat/modele_a.html");
        String template;
        try (InputStream is = tpl.getInputStream()) {
            template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        Formation f = cert.getFormation();
        Utilisateur s = cert.getStagiaire();
        Utilisateur formateur = (f != null) ? f.getFormateur() : null;

        String code = safe(cert.getNumeroUnique());
        String verifyUrl = buildVerifyUrl(code);
        String qrDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(makeQrPng(verifyUrl, 220, 220));

        BigDecimal note = cert.getNoteFinale();
        String noteStr = note == null ? "-" : note.stripTrailingZeros().toPlainString() + "/20";

        LocalDate dd = f != null ? f.getDateDebut() : null;
        LocalDate df = f != null ? f.getDateFin() : null;

        template = template.replace("{{CERT_CODE}}", html(code));
        template = template.replace("{{DATE_EMISSION}}", html(formatDate(cert.getDateObtention())));
        template = template.replace("{{NOM_PRENOM}}", html(s != null ? s.getNomComplet() : "-"));
        template = template.replace("{{FORMATION_NOM}}", html(f != null ? f.getNom() : "-"));
        template = template.replace("{{DATE_DEBUT}}", html(formatDate(dd)));
        template = template.replace("{{DATE_FIN}}", html(formatDate(df)));
        template = template.replace("{{NOTE}}", html(noteStr));
        template = template.replace("{{FORMATEUR}}", html(formateur != null ? formateur.getNomComplet() : "-"));
        template = template.replace("{{VERIFY_URL}}", html(verifyUrl));
        template = template.replace("{{QR_DATA_URI}}", qrDataUri);
        template = template.replace("{{PLATFORM_NAME}}", "FormationCloud");
        return template;
    }

    private static String formatDate(LocalDate d) {
        if (d == null) return "-";
        return DF.format(d);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String html(String s) {
        if (s == null) return "";
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static byte[] makeQrPng(String content, int w, int h) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, w, h);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", os);
        return os.toByteArray();
    }

    public Path defaultPdfPath(String certificatCode) {
        Path dir = Paths.get("uploads").resolve("certificats").toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier uploads/certificats");
        }
        return dir.resolve(certificatCode + ".pdf");
    }
}
