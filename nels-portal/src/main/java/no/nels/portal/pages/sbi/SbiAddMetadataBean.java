package no.nels.portal.pages.sbi;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.constants.OAuthConstant;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.primefaces.model.UploadedFile;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;

@ManagedBean(name = ManagedBeanNames.pages_sbi_add_metadata)
@ViewScoped
public class SbiAddMetadataBean extends ASecureBean{
    private static final Logger logger = LogManager.getLogger(SbiAddMetadataBean.class);

    private UploadedFile file;
    private String path;
    private String token;

    @Override
    public void secure() {
        ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
            {
                add(new AdministratorUser());
                add(new HelpDeskUser());
                add(new NormalUser());
            }
        };
        SecurityFacade.requireSystemUserType(userTypes);
    }

    @Override
    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
            path = URLParametersFacade.getMustUrLParameter("path");


//            JsonObject requestBody = new JsonObject();
//            requestBody.put(no.nels.commons.constants.ConfigName.CLIENT_ID, "nels_portal")
//                    .put(no.nels.commons.constants.ConfigName.CLIENT_SECRET, "pSIXMKS33JsLqu7HoejWl3rHhAdiN4")
//                    .put(no.nels.commons.constants.ConfigName.GRANT_TYPE, "client_credentials")
//                    .put(no.nels.commons.constants.ConfigName.SCOPE, "component");
//            Response response = ClientBuilder.newClient().target("http://test-fe.cbu.uib.no:7600/token").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody.encode()));
//            if (response.getStatus() == HttpResponseStatus.OK.code()) {
//                JsonObject responseBody = new JsonObject(response.readEntity(String.class));
//                token = responseBody.getString(OAuthConstant.ACCESS_TOKEN);
//            } else {
//                logger.error(response.readEntity(String.class));
//                MessageFacade.AddError("Can't upload file now");
//            }
        }
        return "Upload Metadata File";
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void cmdCancel_Click() {
        NavigationFacade.closePopup();
    }

    public void upload() {
        if (file != null) {
            if (file.getFileName().endsWith("xlsx")) {
                try {
                    ClientConfig config = new ClientConfig();
//                Response uploadResponse = ClientBuilder.newClient(config).target("http://test-fe.cbu.uib.no:7607/sbi" + path + "/metadata").request().header(HttpHeaders.AUTHORIZATION.toString(), OAuthConstant.BEARER + " " + token)
//                        .property(ClientProperties.CHUNKED_ENCODING_SIZE, "4096")
//                        .property(ClientProperties.REQUEST_ENTITY_PROCESSING, "CHUNKED")
//                        .post(Entity.entity(file.getInputstream(), MediaType.MULTIPART_FORM_DATA));
                    Response uploadResponse = ClientBuilder.newClient(config).target(Config.getPublicApiUrl() + "/seek/sbi" + path + "/metadata").request()
                            .property(ClientProperties.CHUNKED_ENCODING_SIZE, "4096")
                            .property(ClientProperties.REQUEST_ENTITY_PROCESSING, "CHUNKED")
                            .post(Entity.entity(file.getInputstream(), MediaType.MULTIPART_FORM_DATA));
                    if (uploadResponse.getStatus() != 200) {
                        logger.error(uploadResponse.readEntity(String.class));
                        MessageFacade.AddError("Uploading file failed");
                    } else {
                        MessageFacade.addInfo("Success",
                                "File uploaded successfully", true);
                        NavigationFacade.closePopup();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e.getCause());
                    MessageFacade.AddError("Uploading file failed");
                }
            } else {
                MessageFacade.AddError("Error", "File format not supported");
            }
        } else {
            MessageFacade.AddError("File not provided");
        }
    }
}
