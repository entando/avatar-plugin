package org.entando.plugin.avatar.web.rest;

import org.entando.plugin.avatar.AvatarPluginApp;
import org.entando.plugin.avatar.client.EntandoAuthClient;
import org.entando.plugin.avatar.config.AvatarPluginConfigManager;
import org.entando.plugin.avatar.config.TestSecurityConfiguration;
import org.entando.plugin.avatar.domain.Avatar;
import org.entando.plugin.avatar.repository.AvatarRepository;
import org.entando.plugin.avatar.service.AvatarService;
import org.entando.plugin.avatar.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;

import static org.entando.plugin.avatar.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link AvatarResource} REST controller.
 */
@SpringBootTest(classes = {AvatarPluginApp.class, TestSecurityConfiguration.class})
public class AvatarResourceIT {

    private static final String DEFAULT_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_USERNAME = "BBBBBBBBBB";

    private static final byte[] DEFAULT_IMAGE = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_IMAGE = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_IMAGE_CONTENT_TYPE = "image/png";

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private AvatarService avatarService;

    @Autowired
    private AvatarPluginConfigManager configManager;

    @Autowired
    private EntandoAuthClient authClient;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restAvatarMockMvc;

    private Avatar avatar;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final AvatarResource avatarResource = new AvatarResource(avatarService, configManager, authClient);
        this.restAvatarMockMvc = MockMvcBuilders.standaloneSetup(avatarResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Avatar createEntity(EntityManager em) {
        Avatar avatar = new Avatar()
            .username(DEFAULT_USERNAME)
            .image(DEFAULT_IMAGE)
            .imageContentType(DEFAULT_IMAGE_CONTENT_TYPE);
        return avatar;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Avatar createUpdatedEntity(EntityManager em) {
        Avatar avatar = new Avatar()
            .username(UPDATED_USERNAME)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE);
        return avatar;
    }

    @BeforeEach
    public void initTest() {
        avatar = createEntity(em);
    }

    @Test
    @Transactional
    public void createAvatar() throws Exception {
        int databaseSizeBeforeCreate = avatarRepository.findAll().size();

        // Create the Avatar
        restAvatarMockMvc.perform(post("/api/avatars")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(avatar)))
            .andExpect(status().isCreated());

        // Validate the Avatar in the database
        List<Avatar> avatarList = avatarRepository.findAll();
        assertThat(avatarList).hasSize(databaseSizeBeforeCreate + 1);
        Avatar testAvatar = avatarList.get(avatarList.size() - 1);
        assertThat(testAvatar.getUsername()).isEqualTo(DEFAULT_USERNAME);
        assertThat(testAvatar.getImage()).isEqualTo(DEFAULT_IMAGE);
        assertThat(testAvatar.getImageContentType()).isEqualTo(DEFAULT_IMAGE_CONTENT_TYPE);
    }

    @Test
    @Transactional
    public void createAvatarWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = avatarRepository.findAll().size();

        // Create the Avatar with an existing ID
        avatar.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restAvatarMockMvc.perform(post("/api/avatars")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(avatar)))
            .andExpect(status().isBadRequest());

        // Validate the Avatar in the database
        List<Avatar> avatarList = avatarRepository.findAll();
        assertThat(avatarList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkUsernameIsRequired() throws Exception {
        int databaseSizeBeforeTest = avatarRepository.findAll().size();
        // set the field null
        avatar.setUsername(null);

        // Create the Avatar, which fails.

        restAvatarMockMvc.perform(post("/api/avatars")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(avatar)))
            .andExpect(status().isBadRequest());

        List<Avatar> avatarList = avatarRepository.findAll();
        assertThat(avatarList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllAvatars() throws Exception {
        // Initialize the database
        avatarRepository.saveAndFlush(avatar);

        // Get all the avatarList
        restAvatarMockMvc.perform(get("/api/avatars?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(avatar.getId().intValue())))
            .andExpect(jsonPath("$.[*].username").value(hasItem(DEFAULT_USERNAME.toString())))
            .andExpect(jsonPath("$.[*].imageContentType").value(hasItem(DEFAULT_IMAGE_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].image").value(hasItem(Base64Utils.encodeToString(DEFAULT_IMAGE))));
    }

    @Test
    @Transactional
    public void getAvatar() throws Exception {
        // Initialize the database
        avatarRepository.saveAndFlush(avatar);

        // Get the avatar
        restAvatarMockMvc.perform(get("/api/avatars/{id}", avatar.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(avatar.getId().intValue()))
            .andExpect(jsonPath("$.username").value(DEFAULT_USERNAME.toString()))
            .andExpect(jsonPath("$.imageContentType").value(DEFAULT_IMAGE_CONTENT_TYPE))
            .andExpect(jsonPath("$.image").value(Base64Utils.encodeToString(DEFAULT_IMAGE)));
    }

    @Test
    @Transactional
    public void getNonExistingAvatar() throws Exception {
        // Get the avatar
        restAvatarMockMvc.perform(get("/api/avatars/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateAvatar() throws Exception {
        // Initialize the database
        avatarService.save(avatar);

        int databaseSizeBeforeUpdate = avatarRepository.findAll().size();

        // Update the avatar
        Avatar updatedAvatar = avatarRepository.findById(avatar.getId()).get();
        // Disconnect from session so that the updates on updatedAvatar are not directly saved in db
        em.detach(updatedAvatar);
        updatedAvatar
            .username(UPDATED_USERNAME)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE);

        restAvatarMockMvc.perform(put("/api/avatars")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedAvatar)))
            .andExpect(status().isOk());

        // Validate the Avatar in the database
        List<Avatar> avatarList = avatarRepository.findAll();
        assertThat(avatarList).hasSize(databaseSizeBeforeUpdate);
        Avatar testAvatar = avatarList.get(avatarList.size() - 1);
        assertThat(testAvatar.getUsername()).isEqualTo(UPDATED_USERNAME);
        assertThat(testAvatar.getImage()).isEqualTo(UPDATED_IMAGE);
        assertThat(testAvatar.getImageContentType()).isEqualTo(UPDATED_IMAGE_CONTENT_TYPE);
    }

    @Test
    @Transactional
    public void updateNonExistingAvatar() throws Exception {
        int databaseSizeBeforeUpdate = avatarRepository.findAll().size();

        // Create the Avatar

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAvatarMockMvc.perform(put("/api/avatars")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(avatar)))
            .andExpect(status().isBadRequest());

        // Validate the Avatar in the database
        List<Avatar> avatarList = avatarRepository.findAll();
        assertThat(avatarList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteAvatar() throws Exception {
        // Initialize the database
        avatarService.save(avatar);

        int databaseSizeBeforeDelete = avatarRepository.findAll().size();

        // Delete the avatar
        restAvatarMockMvc.perform(delete("/api/avatars/{id}", avatar.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Avatar> avatarList = avatarRepository.findAll();
        assertThat(avatarList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Avatar.class);
        Avatar avatar1 = new Avatar();
        avatar1.setId(1L);
        Avatar avatar2 = new Avatar();
        avatar2.setId(avatar1.getId());
        assertThat(avatar1).isEqualTo(avatar2);
        avatar2.setId(2L);
        assertThat(avatar1).isNotEqualTo(avatar2);
        avatar1.setId(null);
        assertThat(avatar1).isNotEqualTo(avatar2);
    }
}
