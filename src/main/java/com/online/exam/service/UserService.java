package com.online.exam.service;

import com.online.exam.domain.Authority;
import com.online.exam.domain.User;
import com.online.exam.repository.AuthorityRepository;
import com.online.exam.repository.UserRepository;
import com.online.exam.security.SecurityUtils;
import com.online.exam.service.util.RandomUtil;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private UserRepository userRepository;

    @Inject
    private AuthorityRepository authorityRepository;

    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        userRepository.findOneByActivationKey(key)
            .map(user -> {
                // activate given user for the registration key.
                user.setActivated(true);
                user.setActivationKey(null);
                userRepository.save(user);
                log.debug("Activated user: {}", user);
                return user;
            });
        return Optional.empty();
    }

    public User createUserInformation(String login, String userNo, String password, String firstName, String lastName, String email,
                                      String langKey, List<String> roles, Integer deleted) {
        User newUser = new User();
        log.debug("Created Information for User roles: {}", roles);
        Authority authority = authorityRepository.findOne(roles.get(0));
        Set<Authority> authorities = new HashSet<>();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(login);
        // new user gets initially a generated password
        newUser.setUserNo(userNo);
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setLangKey(langKey);
        // new user is not active
        newUser.setActivated(false);
        newUser.setDeleted(deleted);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        authorities.add(authority);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public void updateUserInformation(
    		String login,
    		String phone,
    		Integer gender,
    		Integer age,
    		String classes,
    		String description,
    		String avatarUrl,
    		String firstName,
    		String lastName,
    		String email) {
        userRepository.findOneByLogin(SecurityUtils.getCurrentLogin()).ifPresent(u -> {
        	u.setLogin(login);
        	u.setPhone(phone);
        	u.setGender(gender);
        	u.setAge(age);
        	u.setClasses(classes);
        	u.setDescription(description);
        	u.setAvatarUrl(avatarUrl);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            userRepository.save(u);
            log.debug("Changed Information for User: {}", u);
        });
    }
    
    public void updateOtherUserInformation(
    		String login,
    		String phone,
    		Integer gender,
    		Integer age,
    		String classes,
    		String description,
    		String avatarUrl,
    		String firstName,
    		String lastName,
    		String email) {
        userRepository.findOneByLogin(login).ifPresent(u -> {
        	log.debug("Changed other Information for User: {}", u);
        	u.setLogin(login);
        	u.setPhone(phone);
        	u.setGender(gender);
        	u.setAge(age);
        	u.setClasses(classes);
        	u.setDescription(description);
        	u.setAvatarUrl(avatarUrl);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            userRepository.save(u);
            log.debug("Changed other Information for User: {}", u);
        });
    }

    public void changePassword(String password) {
        userRepository.findOneByLogin(SecurityUtils.getCurrentLogin()).ifPresent(u-> {
            String encryptedPassword = passwordEncoder.encode(password);
            u.setPassword(encryptedPassword);
            userRepository.save(u);
            log.debug("Changed password for User: {}", u);
        });
    }

    @Transactional(readOnly = true)
    public User getUserWithAuthorities() {
        User currentUser = userRepository.findOneByLogin(SecurityUtils.getCurrentLogin()).get();
        currentUser.getAuthorities().size(); // eagerly load the association
        return currentUser;
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p/>
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     * </p>
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        DateTime now = new DateTime();
        List<User> users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(now.minusDays(3));
        for (User user : users) {
            log.debug("Deleting not activated user {}", user.getLogin());
            userRepository.delete(user);
        }
    }

    public void deleteUserByAdmin(Long id) {
        log.debug("Deleting user logicly by id: {}", id);
        userRepository.deleteUserLogic(id);
    }

    public void upadtePasswordById(String password) {
        userRepository.findOneByLogin(SecurityUtils.getCurrentLogin()).ifPresent(u-> {
            String encryptedPassword = passwordEncoder.encode(password);
            Long id = u.getId();
            u.setPassword(encryptedPassword);
            userRepository.upadtePasswordById(encryptedPassword, id);
        });
    }
}
