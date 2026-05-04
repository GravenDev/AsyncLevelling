package fr.redstom.asynclevelling.jpa.services;

import fr.redstom.asynclevelling.jpa.entities.GuildDao;
import fr.redstom.asynclevelling.jpa.entities.GuildSettingsDao;
import fr.redstom.asynclevelling.jpa.entities.MemberDao;
import fr.redstom.asynclevelling.jpa.entities.UserDao;
import fr.redstom.asynclevelling.jpa.repositories.MemberRepository;
import fr.redstom.asynclevelling.utils.LevelUtils;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Lazy)
public class MemberService {

    private final LevelUtils levelUtils;

    private final MemberRepository memberRepository;

    private final GuildService guildService;
    private final UserService userService;
    private final GuildRewardService rewardService;
    private final GuildNotificationService notificationService;
    private final GuildSettingsService settingsService;

    private final JDA jda;

    @Value("${xp.timeout}")
    private int timeout = 60;

    @Transactional
    public MemberDao getMemberByDiscordMember(Member member) {
        UserDao user = userService.getOrCreateByDiscordUser(member.getUser());
        GuildDao guild = guildService.getOrCreateByDiscordGuild(member.getGuild());

        return memberRepository
                .findByUserAndGuild(user, guild)
                .orElseGet(() -> this.createMember(member, user, guild));
    }

    @Transactional
    public MemberDao getMemberByGuildAndMemberId(Guild guild, long userId, long baseLevel) {
        UserDao user = userService.getOrCreateByUserId(userId);
        GuildDao gGuild = guildService.getOrCreateByDiscordGuild(guild);

        return memberRepository
                .findByUserAndGuild(user, gGuild)
                .orElseGet(
                        () ->
                                memberRepository.save(
                                        MemberDao.builder()
                                                .user(user)
                                                .guild(gGuild)
                                                .level(baseLevel)
                                                .build()));
    }

    private MemberDao createMember(Member member, UserDao user, GuildDao guild) {
        GuildSettingsDao settings = settingsService.getOrCreateByGuild(member.getGuild());

        AtomicLong level = new AtomicLong();
        if (settings.autoLevelGrant()) {
            for (Role role : member.getRoles()) {
                rewardService
                        .getByMemberRole(member, role)
                        .ifPresent(
                                reward -> {
                                    if (reward.level() > level.get()) {
                                        level.set(reward.level());
                                    }
                                });
            }
        }

        return memberRepository.save(
                MemberDao.builder().user(user).guild(guild).level(level.get()).build());
    }

    @Transactional
    public boolean addXpFromMessage(Member member, Message message) {
        if (settingsService.getOrCreateByGuild(member.getGuild()).pause()) {
            return false;
        }

        MemberDao gMember = getMemberByDiscordMember(member);

        Instant messageCreated = message.getTimeCreated().toInstant();

        long distance = ChronoUnit.SECONDS.between(messageCreated, gMember.lastMessageAt());
        if (Math.abs(distance) < timeout) return false;

        long xpToGain = levelUtils.flattenMessageLengthIntoGain(message.getContentRaw().length());
        if (member.isBoosting()) {
            xpToGain = Math.round(xpToGain * 1.5d);
        }

        gMember.experience(gMember.experience() + xpToGain);
        gMember.lastMessageAt(messageCreated);

        checkLevel(member);
        memberRepository.save(gMember);

        log.info("{} got added {} xp from message.", member.getUser().getAsTag(), xpToGain);
        return true;
    }

    @Transactional
    public void addXp(Member member, long amount, String reason) {
        if (settingsService.getOrCreateByGuild(member.getGuild()).pause()) {
            return;
        }

        MemberDao gMember = getMemberByDiscordMember(member);

        if (member.isBoosting()) {
            amount = Math.round(amount * 1.5d);
        }

        gMember.experience(gMember.experience() + amount);
        memberRepository.save(gMember);
        log.info(
                "{} got added {} xp for {} in guild {}.",
                member.getUser().getAsTag(),
                amount,
                reason,
                member.getGuild().getName());

        checkLevel(member);
    }

    @Transactional
    public void addXp(Member member, long amount) {
        addXp(member, amount, "Unknown reason");
    }

    @Transactional
    public boolean checkLevel(Member member) {
        MemberDao gMember = getMemberByDiscordMember(member);

        long xp = gMember.experience();
        long xpToNextLevel = levelUtils.xpForNextLevelAt(gMember.level());

        if (xp < xpToNextLevel) {
            return false;
        }

        do {
            gMember.experience(gMember.experience() - xpToNextLevel);
            gMember.level(gMember.level() + 1);

            xpToNextLevel = levelUtils.xpForNextLevelAt(gMember.level());
        } while (gMember.experience() > xpToNextLevel);

        rewardService.grantReward(member, gMember.level());
        notificationService.sendNotification(member, gMember.level());

        memberRepository.save(gMember);
        log.info(
                "{} has levelled up to level {} in guild {}.",
                member.getUser().getAsTag(),
                gMember.level(),
                gMember.level());

        return true;
    }

    @Transactional
    public int getRank(Member member) {
        MemberDao gMember = getMemberByDiscordMember(member);
        return memberRepository.findPositionOfMember(gMember.user(), gMember.guild());
    }

    @Nullable
    public Member getDiscordMemberByMember(MemberDao gMember) {
        Guild guild = jda.getGuildById(gMember.guild().id());
        Member member = guild.getMemberById(gMember.user().id());

        if (member == null) {
            try {
                member = guild.retrieveMemberById(gMember.user().id()).useCache(true).complete();
            } catch (Exception e) {
                return null;
            }
        }

        return member;
    }
}
