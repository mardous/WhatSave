/*
 * Copyright (C) 2024 Christians Martínez Alvarado
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 * the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package com.simplified.wsstatussaver.fragments.playback.video

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.button.MaterialButton
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.FragmentVideoBinding
import com.simplified.wsstatussaver.fragments.playback.PlaybackChildFragment

/**
 * @author Christians M. A. (mardous)
 */
class VideoFragment : PlaybackChildFragment(R.layout.fragment_video), Player.Listener {

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!
    private val playerView get() = binding.playerView

    private var player: ExoPlayer? = null

    override val saveButton: MaterialButton
        get() = playerView.findViewById(R.id.save)

    override val shareButton: MaterialButton
        get() = playerView.findViewById(R.id.share)

    override val deleteButton: MaterialButton
        get() = playerView.findViewById(R.id.delete)

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVideoBinding.bind(view)
        playerView.controllerAutoShow = false
        playerView.controllerShowTimeoutMs = 3000
        initPlayer()
    }

    override fun onStart() {
        super.onStart()
        preparePlayer()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        player?.stop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
        player?.removeListener(this)
        player?.release()
        player = null
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            playerView.player = player
            if (isResumed) {
                player?.play()
            }
        }
    }

    private fun initPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(requireContext())
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private fun preparePlayer() {
        player?.addListener(this)
        player?.setMediaItem(MediaItem.fromUri(status.fileUri))
        player?.repeatMode = Player.REPEAT_MODE_ONE
        player?.prepare()
    }
}