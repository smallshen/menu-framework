package com.heroslender.hmf.bukkit.sdk.nms.v1_8

import com.heroslender.hmf.bukkit.sdk.nms.PacketInterceptor
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import java.lang.reflect.Field

class PacketInterceptorImpl(
    override val player: Player,
    override val handler: PacketInterceptor.PacketInterceptorHandler,
) : ChannelDuplexHandler(), PacketInterceptor {

    init {
        val channel = (player as CraftPlayer).handle.playerConnection.networkManager.channel
        if (channel != null) {
            if (channel.pipeline()[HANDLER_ID] != null) {
                channel.pipeline().remove(HANDLER_ID)
            }

            channel.pipeline().addBefore("packet_handler", HANDLER_ID, this)
        }
        // TODO Use the channel directly to send packets in the future
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is PacketPlayInUseEntity) {
            val entityId: Int = entityIdField.get(msg) as Int
            val action = when (msg.a()) {
                PacketPlayInUseEntity.EnumEntityUseAction.ATTACK -> PacketInterceptor.Action.LEFT_CLICK
                else -> PacketInterceptor.Action.RIGHT_CLICK
            }

            val handled = handler.handleInteraction(player, entityId, action)
            if (handled) {
                return
            }
        }

        super.channelRead(ctx, msg)
    }

    companion object {
        const val HANDLER_ID = "hmf_packet_handler"

        val entityIdField: Field = PacketPlayInUseEntity::class.java.getDeclaredField("a").apply {
            isAccessible = true
        }
    }
}