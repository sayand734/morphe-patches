/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.music.patches.components;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.patches.components.BufferAsciiStrings;
import app.morphe.extension.shared.patches.components.ContextInterface;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class ActionButtonsFilter extends Filter {

    private static final String VIDEO_ACTION_BAR_PREFIX = "video_action_bar.e";
    private static final String VIDEO_ACTION_BUTTON_WRAPPER_PREFIX = "video_action_button_with_vm_input.e";

    private static final String COMMENTS_MARKER = "music-comment-panel";
    private static final String LYRICS_MARKER = "music_watch_lyrics_panel";
    private static final String SHARE_MARKER = "timestamp_share_switch_button_entity_key";
    private static final String RADIO_MARKER = "RDAMVM";

    private final StringFilterGroup actionBar;
    private final StringFilterGroup genericActionButton;

    public ActionButtonsFilter() {
        actionBar = new StringFilterGroup(
                Settings.HIDE_ACTION_BAR,
                VIDEO_ACTION_BAR_PREFIX
        );
        addIdentifierCallbacks(actionBar);

        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_LIKE_DISLIKE_BUTTON,
                        "like_button.e",
                        "dislike_button.e",
                        "segmented_like_dislike_button.e"
                ),
                new StringFilterGroup(
                        Settings.HIDE_DOWNLOAD_BUTTON,
                        "download_button.e",
                        "music_download_button.e"
                )
        );

        // Comments, Lyrics, Share, Save and Radio all render as generic `button.e` inside
        // `video_action_button_with_vm_input.e` - identity is dispatched inside isFiltered
        // from the button's ascii-string buffer.
        genericActionButton = new StringFilterGroup(
                null,
                VIDEO_ACTION_BUTTON_WRAPPER_PREFIX
        );
        addPathCallbacks(genericActionButton);
    }

    @Override
    public boolean isFiltered(ContextInterface contextInterface,
                              String identifier,
                              String accessibility,
                              String path,
                              byte[] buffer,
                              BufferAsciiStrings asciiStrings,
                              StringFilterGroup matchedGroup,
                              FilterContentType contentType,
                              int contentIndex) {
        if (matchedGroup == actionBar) {
            return true;
        }
        if (matchedGroup == genericActionButton) {
            if (path == null || !path.contains(VIDEO_ACTION_BAR_PREFIX)) {
                return false;
            }
            // Wrapper- and inner-level renders embed sibling data in their proto buffer, so
            // a single marker would match the whole action bar. Only fire on the leaf-button
            // render - path contains `|button.eml-fe|` but not the `button_inner` descendant.
            if (!path.contains("|button.eml-fe|") || path.contains("button_inner")) {
                return false;
            }
            if (asciiStrings == null) {
                return false;
            }
            // Dispatch by endpoint marker. Each of the four known buttons has a unique
            // endpoint string in its buffer. The Save button has no unique endpoint of its
            // own - its icon (`_playlist_add_`) also appears in every button's shared icon
            // catalogue - so it is resolved by elimination as the fall-through branch.
            String strings = asciiStrings.getStrings();
            if (strings.contains(COMMENTS_MARKER)) {
                return Settings.HIDE_COMMENTS_BUTTON.get();
            }
            if (strings.contains(LYRICS_MARKER)) {
                return Settings.HIDE_LYRICS_BUTTON.get();
            }
            if (strings.contains(SHARE_MARKER)) {
                return Settings.HIDE_SHARE_BUTTON.get();
            }
            if (strings.contains(RADIO_MARKER)) {
                return Settings.HIDE_RADIO_BUTTON.get();
            }
            return Settings.HIDE_SAVE_BUTTON.get();
        }
        return path != null && path.contains(VIDEO_ACTION_BAR_PREFIX);
    }
}
