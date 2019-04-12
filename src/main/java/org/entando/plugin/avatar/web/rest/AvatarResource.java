package org.entando.plugin.avatar.web.rest;
import org.entando.plugin.avatar.domain.Avatar;
import org.entando.plugin.avatar.service.AvatarService;
import org.entando.plugin.avatar.web.rest.errors.BadRequestAlertException;
import org.entando.plugin.avatar.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Avatar.
 */
@RestController
@RequestMapping("/api")
public class AvatarResource {

    private final Logger log = LoggerFactory.getLogger(AvatarResource.class);

    private static final String ENTITY_NAME = "avatarPluginAvatar";

    private final AvatarService avatarService;

    public AvatarResource(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    /**
     * POST  /avatars : Create a new avatar.
     *
     * @param avatar the avatar to create
     * @return the ResponseEntity with status 201 (Created) and with body the new avatar, or with status 400 (Bad Request) if the avatar has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/avatars")
    public ResponseEntity<Avatar> createAvatar(@Valid @RequestBody Avatar avatar) throws URISyntaxException {
        log.debug("REST request to save Avatar : {}", avatar);
        if (avatar.getId() != null) {
            throw new BadRequestAlertException("A new avatar cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Avatar result = avatarService.save(avatar);
        return ResponseEntity.created(new URI("/api/avatars/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /avatars : Updates an existing avatar.
     *
     * @param avatar the avatar to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated avatar,
     * or with status 400 (Bad Request) if the avatar is not valid,
     * or with status 500 (Internal Server Error) if the avatar couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/avatars")
    public ResponseEntity<Avatar> updateAvatar(@Valid @RequestBody Avatar avatar) throws URISyntaxException {
        log.debug("REST request to update Avatar : {}", avatar);
        if (avatar.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Avatar result = avatarService.save(avatar);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, avatar.getId().toString()))
            .body(result);
    }

    /**
     * GET  /avatars : get all the avatars.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of avatars in body
     */
    @GetMapping("/avatars")
    public List<Avatar> getAllAvatars() {
        log.debug("REST request to get all Avatars");
        return avatarService.findAll();
    }

    /**
     * GET  /avatars/:id : get the "id" avatar.
     *
     * @param id the id of the avatar to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the avatar, or with status 404 (Not Found)
     */
    @GetMapping("/avatars/{id}")
    public ResponseEntity<Avatar> getAvatar(@PathVariable Long id) {
        log.debug("REST request to get Avatar : {}", id);
        Optional<Avatar> avatar = avatarService.findOne(id);
        return ResponseUtil.wrapOrNotFound(avatar);
    }

    /**
     * DELETE  /avatars/:id : delete the "id" avatar.
     *
     * @param id the id of the avatar to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/avatars/{id}")
    public ResponseEntity<Void> deleteAvatar(@PathVariable Long id) {
        log.debug("REST request to delete Avatar : {}", id);
        avatarService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
