package org.entando.plugin.avatar.web.rest;

import org.apache.commons.io.IOUtils;
import org.entando.plugin.avatar.client.EntandoAuthClient;
import org.entando.plugin.avatar.config.AvatarPluginConfig;
import org.entando.plugin.avatar.config.AvatarPluginConfigManager;
import org.entando.plugin.avatar.domain.Avatar;
import org.entando.plugin.avatar.domain.AvatarStyle;
import org.entando.plugin.avatar.service.AvatarService;
import org.entando.plugin.avatar.service.impl.AvatarUploadException;
import org.entando.plugin.avatar.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link org.entando.plugin.avatar.domain.Avatar}.
 */
@RestController
@RequestMapping("/api")
public class AvatarResource {

    private final Logger log = LoggerFactory.getLogger(AvatarResource.class);

    private static final String ENTITY_NAME = "avatarPluginAvatar";
    private static final String FILE_PARAM = "data";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AvatarPluginConfigManager configManager;
    private final AvatarService avatarService;
    private final EntandoAuthClient authClient;

    public AvatarResource(AvatarService avatarService,
        AvatarPluginConfigManager configManager,
        EntandoAuthClient authClient ) {
        this.avatarService = avatarService;
        this.configManager = configManager;
        this.authClient = authClient;
    }

    @PostMapping("/avatars/image/{userId}")
    public ResponseEntity createAvatar(@PathVariable("userId") String userId,
            @RequestParam(FILE_PARAM) MultipartFile image) throws IOException {

        try {
            avatarService.upload(userId, image);
            return ResponseEntity.ok().build();
        } catch (AvatarUploadException exception) {
            throw new BadRequestAlertException(exception.getMessage(), ENTITY_NAME, "upload");
        }
    }

    @GetMapping("/avatars/image/{userId}")
    public ResponseEntity<?> getImage(@PathVariable("userId") String userId, HttpServletResponse response)
            throws IOException {

        if (configManager.getAvatarPluginConfig().getStyle() == AvatarStyle.GRAVATAR) {
            return returnGravatarImage(userId, response);
        }

        return returnLocalImage(userId, response);
    }

    @PostMapping("/avatars/image/currentUser")
    public ResponseEntity getCurrentUserAvatar(Principal user,
            @RequestParam(FILE_PARAM) MultipartFile image, HttpServletResponse response) throws IOException {
        return this.createAvatar(user.getName(), image);
    }

    @GetMapping("/avatars/image/currentUser")
    public ResponseEntity getCurrentUserAvatar(Principal user, HttpServletResponse response)
            throws IOException {
        return this.getImage(user.getName(), response);
    }

    private ResponseEntity<?> returnGravatarImage(String userId, HttpServletResponse response) throws IOException {

        RestTemplate restTemplate = new RestTemplate();

        String email = authClient.getUserDetail(userId).getEmail();

        ResponseEntity<Resource> gravatarResponse = restTemplate.exchange(getAvatarUrl(email), HttpMethod.GET, null,
                Resource.class);

        if (!gravatarResponse.getStatusCode().is2xxSuccessful()) {
            return new ResponseEntity<>(gravatarResponse.getStatusCode());
        }

        if (gravatarResponse.getBody() == null) {
            throw new RestClientException("Gravatar resource has returned null");
        }

        response.setContentType(String.valueOf(gravatarResponse.getHeaders().getContentType()));
        try (InputStream in = gravatarResponse.getBody().getInputStream()) {
            IOUtils.copy(in, response.getOutputStream());
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String getAvatarUrl(String email) {

        AvatarPluginConfig avatarConfig = configManager.getAvatarPluginConfig();

        String url = avatarConfig.getGravatarUrl();

        if (!url.endsWith("/")) {
            url += "/";
        }

        url += DigestUtils.md5DigestAsHex(email.getBytes()) + "?d=404&s=" + avatarConfig.getImageWidth();

        return url;
    }

    private ResponseEntity<?> returnLocalImage(String username, HttpServletResponse response) throws IOException {

        Optional<Avatar> maybeAvatar = avatarService.findByUsername(username);

        if (!maybeAvatar.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Avatar avatar = maybeAvatar.get();

        response.setContentType(avatar.getImageContentType());
        try (ByteArrayInputStream in = new ByteArrayInputStream(avatar.getImage())) {
            IOUtils.copy(in, response.getOutputStream());
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/avatars/image/{username}")
    public ResponseEntity<Void> deleteAvatar(@PathVariable("username") String username) {
        avatarService.deleteByUsername(username);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * {@code POST  /avatars} : Create a new avatar.
     *
     * @param avatar the avatar to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     *         body the new avatar, or with status {@code 400 (Bad Request)} if the
     *         avatar has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/avatars")
    public ResponseEntity<Avatar> createAvatar(@Valid @RequestBody Avatar avatar) throws URISyntaxException {
        log.debug("REST request to save Avatar : {}", avatar);
        if (avatar.getId() != null) {
            throw new BadRequestAlertException("A new avatar cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Avatar result = avatarService.save(avatar);
        return ResponseEntity
                .created(new URI("/api/avatars/" + result.getId())).headers(HeaderUtil
                        .createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                .body(result);
    }

    /**
     * {@code PUT  /avatars} : Updates an existing avatar.
     *
     * @param avatar the avatar to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the updated avatar, or with status {@code 400 (Bad Request)} if the
     *         avatar is not valid, or with status
     *         {@code 500 (Internal Server Error)} if the avatar couldn't be
     *         updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/avatars")
    public ResponseEntity<Avatar> updateAvatar(@Valid @RequestBody Avatar avatar) throws URISyntaxException {
        log.debug("REST request to update Avatar : {}", avatar);
        if (avatar.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Avatar result = avatarService.save(avatar);
        return ResponseEntity.ok().headers(
                HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, avatar.getId().toString()))
                .body(result);
    }

    /**
     * {@code GET  /avatars} : get all the avatars.
     *
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
     *         of avatars in body.
     */
    @GetMapping("/avatars")
    public List<Avatar> getAllAvatars() {
        log.debug("REST request to get all Avatars");
        return avatarService.findAll();
    }

    /**
     * {@code GET  /avatars/:id} : get the "id" avatar.
     *
     * @param id the id of the avatar to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the avatar, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/avatars/{id}")
    public ResponseEntity<Avatar> getAvatar(@PathVariable Long id) {
        log.debug("REST request to get Avatar : {}", id);
        Optional<Avatar> avatar = avatarService.findOne(id);
        return ResponseUtil.wrapOrNotFound(avatar);
    }

    /**
     * {@code DELETE  /avatars/:id} : delete the "id" avatar.
     *
     * @param id the id of the avatar to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/avatars/{id}")
    public ResponseEntity<Void> deleteAvatar(@PathVariable Long id) {
        log.debug("REST request to delete Avatar : {}", id);
        avatarService.delete(id);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
                .build();
    }
}
