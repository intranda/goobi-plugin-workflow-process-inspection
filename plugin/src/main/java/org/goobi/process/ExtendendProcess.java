package org.goobi.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Process;
import org.goobi.production.cli.helper.StringPair;

import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.NIOFileUtils;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.metadaten.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExtendendProcess implements DatabaseObject {

    @Getter
    @Setter
    private String selectedAction;

    private boolean showFulltext = true;
    private boolean showThumbnail = true;

    private String fulltext;

    @Getter
    private Process process;

    public ExtendendProcess(Process p, String defaultValue) {
        process = p;
        selectedAction = defaultValue;
    }

    @Override
    public void lazyLoad() {
        // nothing
    }

    public String getThumbnailUrl(int size) {
        if (showThumbnail) {
            try {
                String thumbnail = getRepresentativeImageAsString();
                Path imagePath = Paths.get(thumbnail);
                if (StorageProvider.getInstance().isFileExists(imagePath)) {
                    Image image = new Image(process, imagePath.getParent().getFileName().toString(), imagePath.getFileName().toString(), 0, size);
                    return image.getThumbnailUrl();
                } else {
                    FacesContext context = FacesContextHelper.getCurrentFacesContext();
                    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                    String scheme = request.getScheme(); // http
                    String serverName = request.getServerName(); // hostname.com
                    int serverPort = request.getServerPort(); // 80
                    String contextPath = request.getContextPath(); // /mywebapp
                    StringBuilder sb = new StringBuilder();
                    sb.append(scheme);
                    sb.append("://");
                    sb.append(serverName);
                    sb.append(":");
                    sb.append(serverPort);
                    sb.append(contextPath);
                    sb.append("/");
                    sb.append(thumbnail);
                    sb.append("&amp;width=");
                    sb.append(size);
                    sb.append("&amp;height=");
                    sb.append(size);
                    return sb.toString();
                }
            } catch (IOException | SwapException | DAOException e) {
                log.error("Error creating representative image url for process " + process.getId());
            }
        }
        return "";
    }

    public String getFulltext() {
        if (showFulltext && StringUtils.isBlank(fulltext)) {
            try {
                Path txtFolder = Paths.get(process.getOcrTxtDirectory());
                if (StorageProvider.getInstance().isDirectory(txtFolder)) {
                    List<Path> files = StorageProvider.getInstance().listFiles(txtFolder.toString());
                    if (!files.isEmpty()) {
                        Path firstTxtFile = files.get(0);
                        StringBuilder response = new StringBuilder();
                        String buffer = null;
                        try (BufferedReader in = new BufferedReader(
                                new InputStreamReader(StorageProvider.getInstance().newInputStream(firstTxtFile), StandardCharsets.UTF_8))) {
                            while ((buffer = in.readLine()) != null && response.length() < 500) {
                                response.append(buffer.replaceAll("(\\s+)", " ")).append("<br/>");
                            }
                        }
                        fulltext = response.toString();
                        if (fulltext.length() > 500) {
                            fulltext = fulltext.substring(0, 500);
                            fulltext = fulltext.substring(0, fulltext.lastIndexOf(" "));
                        }
                    }
                }

            } catch (SwapException | IOException e) {
                log.error(e);
            }
        }
        return fulltext;

    }

    public String getMetadataValue(String metadataName) {
        for (StringPair sp : process.getMetadataList()) {
            if (sp.getOne().equals(metadataName)) {
                return sp.getTwo();
            }
        }
        return "";
    }

    private String getRepresentativeImageAsString() {
        String representativeImage = "";

        // check if thumb folder contains an image - use it
        try {
            Path thumbsDirectory = Paths.get(process.getImagesDirectory(), process.getTitel() + "_thumbs");

            if (StorageProvider.getInstance().isFileExists(thumbsDirectory)) {
                List<Path> images = StorageProvider.getInstance().listFiles(thumbsDirectory.toString(), NIOFileUtils.imageNameFilter);
                if (!images.isEmpty()) {
                    return images.get(0).toString();
                }
            }
        } catch (IOException | SwapException e) {
            log.error(e);
        }
        // else check file extension of first file in media folder
        try {
            List<Path> images = StorageProvider.getInstance().listFiles(process.getImagesTifDirectory(true));
            if (!images.isEmpty()) {
                representativeImage = images.get(0).toString();
            }
        } catch (IOException | SwapException e) {
            log.error(e);
        }

        String extension = representativeImage.substring(representativeImage.lastIndexOf(".") + 1);

        switch (extension.toLowerCase()) {
            case "pdf":
                representativeImage = "uii/template/img/goobi_3d_object_placeholder.png?version=1";
                break;
            case "epub":
            case "mobi":
            case "azw":
                representativeImage = "uii/thumbnail_epub.jpg?version=1";
                break;
            default:
                representativeImage = "uii/template/img/goobi_3d_object_placeholder.png?version=1";

        }

        return representativeImage;
    }

}
