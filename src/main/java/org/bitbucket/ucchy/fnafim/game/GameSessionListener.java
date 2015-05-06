/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.fnafim.game;

import org.bitbucket.ucchy.fnafim.FiveNightsAtFreddysInMinecraft;
import org.bitbucket.ucchy.fnafim.effect.HideNametagEffect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * リスナークラス
 * @author ucchy
 */
public class GameSessionListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        // セッションが無いならイベントを無視
        GameSession session = FiveNightsAtFreddysInMinecraft.getInstance().getGameSession();
        if ( session == null ) {
            return;
        }

        // 観客なら全てをキャンセル
        if ( session.isSpectator(player) ) {
            event.setCancelled(true);
            return;
        }

        // 参加者ではないならイベントを無視
        if ( !session.isEntrant(player) ) {
            return;
        }

        // 感圧板イベント、左クリックイベントなら、イベントを無視
        if ( event.getAction() == Action.PHYSICAL
                || event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK ) {
            return;
        }

        // アイテムの使用判定は、セッションの方に送って処理する。
        // falseが返されたらイベントをキャンセルする。
        if ( !session.onEntrantInteract(player) ) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        // ネームタグ隠し用のスライムなら、ダメージを無効化
        if ( event.getEntity() instanceof Slime ) {
            Slime slime = (Slime)event.getEntity();
            if ( slime.hasMetadata(HideNametagEffect.TYPE) ) {
                event.setCancelled(true);
                return;
            }
        }

        // 以降はプレイヤーに対してのみ実行する
        if ( !(event.getEntity() instanceof Player) ) {
            return;
        }

        Player player = (Player)event.getEntity();

        // セッションが無いならイベントを無視
        GameSession session = FiveNightsAtFreddysInMinecraft.getInstance().getGameSession();
        if ( session == null ) {
            return;
        }

        // 参加者または観客なら、全てのダメージを無効化
        if ( session.isEntrant(player) || session.isSpectator(player) ) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // セッションが無いならイベントを無視
        GameSession session = FiveNightsAtFreddysInMinecraft.getInstance().getGameSession();
        if ( session == null ) {
            return;
        }

        // ダメージを受けたのがプレイヤーでないならイベントを無視
        Entity damagent = event.getEntity();
        if ( damagent instanceof Slime
                && damagent.hasMetadata(HideNametagEffect.TYPE) ) {
            damagent = damagent.getVehicle();
        }
        if ( !(damagent instanceof Player) ) {
            return;
        }
        Player target = (Player)damagent;

        // 参加者ではないならイベントを無視
        if ( !session.isEntrant(target) ) {
            return;
        }

        // イベントはこの時点で全てキャンセル
        event.setCancelled(true);

        // タッチしたプレイヤーも参加者プレイヤーである場合は、セッションで処理
        if ( event.getDamager() instanceof Player ) {
            Player damager = (Player)event.getDamager();
            if ( !session.isEntrant(damager) ) {
                return;
            }
            session.onTouch(damager, target);
            return;
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        // セッションが無いならイベントを無視
        GameSession session = FiveNightsAtFreddysInMinecraft.getInstance().getGameSession();
        if ( session == null ) {
            return;
        }

        // 参加者または観客なら、全てのイベントをキャンセル
        if ( session.isEntrant(player) || session.isSpectator(player) ) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        // セッションが無いならイベントを無視
        GameSession session = FiveNightsAtFreddysInMinecraft.getInstance().getGameSession();
        if ( session == null ) {
            return;
        }

        // ゲーム中でないならイベントを無視
        if ( session.getPhase() != GameSessionPhase.IN_GAME ) {
            return;
        }

        // Foxyではないならイベントを無視
        if ( !session.isFoxy(player) ) {
            return;
        }

        // リスポーン地点に飛ばして、バインドを設定する
        session.onFoxyMovementEnd();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // セッションが無いならイベントを無視
        GameSession session = FiveNightsAtFreddysInMinecraft.getInstance().getGameSession();
        if ( session == null ) {
            return;
        }

        // ゲーム中でないならイベントを無視
        if ( session.getPhase() != GameSessionPhase.IN_GAME ) {
            return;
        }

        // プレイヤーではないならイベントを無視
        if ( !session.isPlayer(player) ) {
            return;
        }

        // 電力切れプレイヤーなら、即座に脱落させる
        if ( session.getPowerLevel(player) == 0 ) {
            session.onCaughtPlayer(player, null);
        }
    }
}