package com.scs.splitscreenfps.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.scs.basicecs.AbstractEntity;
import com.scs.splitscreenfps.Settings;
import com.scs.splitscreenfps.game.Game;
import com.scs.splitscreenfps.game.ViewportData;
import com.scs.splitscreenfps.game.components.AnimatedComponent;
import com.scs.splitscreenfps.game.components.AnimatedForAvatarComponent;
import com.scs.splitscreenfps.game.components.CanCarry;
import com.scs.splitscreenfps.game.components.CanCollect;
import com.scs.splitscreenfps.game.components.CollidesComponent;
import com.scs.splitscreenfps.game.components.HasModel;
import com.scs.splitscreenfps.game.components.MovementData;
import com.scs.splitscreenfps.game.components.PositionData;
import com.scs.splitscreenfps.game.decals.ShadedGroupStrategy;
import com.scs.splitscreenfps.game.input.IInputMethod;

import ssmith.libgdx.MyBoundingBox;

public class PlayersAvatar extends AbstractEntity {

	private static final float moveSpeed = 2f;
	public static final float playerHeight = 0.4f;

	public Camera camera;
	public CameraController cameraController;
	private Vector3 tmpVector = new Vector3();
	private float footstepTimer;

	private MovementData movementData;
	private PositionData positionData;
	private IInputMethod inputMethod;

	public DecalBatch batch;
	
	private Quaternion qRot = new Quaternion();

	public PlayersAvatar(int idx, ViewportData _viewportData, IInputMethod _inputMethod) {
		super(PlayersAvatar.class.getSimpleName() + "_" + idx);

		inputMethod = _inputMethod;

		this.movementData = new MovementData(0.5f);
		this.addComponent(movementData);
		this.positionData = new PositionData(); // Centre of the player, but NOT where the camera is!
		this.addComponent(positionData);
		this.addComponent(new CanCollect());
		this.addComponent(new CanCarry());
		this.addComponent(new CollidesComponent(false, new MyBoundingBox(positionData.position, .3f, Settings.PLAYER_HEIGHT/2, .3f)));

		// Model stuff
		{
			AssetManager am = new AssetManager();
			am.load("models/KnightCharacter.g3dj", Model.class);
			am.finishLoading();
			Model model = am.get("models/KnightCharacter.g3dj");

			ModelInstance instance = new ModelInstance(model);
			instance.transform.scl(.002f);
			HasModel hasModel = new HasModel(instance);
			hasModel.playerViewId = idx;
			this.addComponent(hasModel);

			AnimationController animation = new AnimationController(instance);
			//String id = model.animations.get(0).id;
			//animation.animate(id, 200, 1f, null, 0.2f); // First anim
			AnimatedComponent anim = new AnimatedComponent();
			anim.animationController = animation;
			this.addComponent(anim);
			
			AnimatedForAvatarComponent avatarAnim = new AnimatedForAvatarComponent();
			avatarAnim.idle_anim = "HumanArmature|Idle";
			avatarAnim.walk_anim = "HumanArmature|Walking";			
			this.addComponent(avatarAnim);
		}


		camera = _viewportData.camera;

		cameraController = new CameraController(camera, inputMethod);

		ShadedGroupStrategy groupStrategy = new ShadedGroupStrategy(camera);
		batch = new DecalBatch(groupStrategy);

	}


	public Vector3 getPosition() {
		return this.positionData.position;
	}


	public void update() {
		checkMovementInput();
		cameraController.update();

		// Rotate model if it has one
		HasModel hasModel = (HasModel)getComponent(HasModel.class);
		if (hasModel != null) {
			//hasModel.model.transform.translate(position);
			//Quaternion q = hasModel.model.transform.getRotation(qRot);
			PositionData pos = (PositionData)getComponent(PositionData.class);
			hasModel.model.transform.setTranslation(pos.position);
		}

	}


	private void checkMovementInput() {
		float delta = Gdx.graphics.getDeltaTime();

		this.movementData.offset.setZero();

		//Movement
		if (this.inputMethod.isForwardsPressed()) { // Gdx.input.isKeyPressed(Input.Keys.W)) {
			tmpVector.set(camera.direction);
			tmpVector.y = 0;
			this.movementData.offset.add(tmpVector.nor().scl(delta * moveSpeed));
		} else if (this.inputMethod.isBackwardsPressed()) { //if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			tmpVector.set(camera.direction);
			tmpVector.y = 0;
			this.movementData.offset.add(tmpVector.nor().scl(delta * -moveSpeed));
		}
		if (this.inputMethod.isStrafeLeftPressed()) { //if (Gdx.input.isKeyPressed(Input.Keys.A)){
			tmpVector.set(camera.direction).crs(camera.up);
			tmpVector.y = 0;
			this.movementData.offset.add(tmpVector.nor().scl(delta * -moveSpeed));
		} else if (this.inputMethod.isStrafeRightPressed()) { //if (Gdx.input.isKeyPressed(Input.Keys.D)){
			tmpVector.set(camera.direction).crs(camera.up);
			tmpVector.y = 0;
			this.movementData.offset.add(tmpVector.nor().scl(delta * moveSpeed));
		}

		camera.position.set(getPosition().x, getPosition().y + playerHeight, getPosition().z);
		
		AnimatedComponent anim = (AnimatedComponent)this.getComponent(AnimatedComponent.class);
		AnimatedForAvatarComponent avatarAnim = (AnimatedForAvatarComponent)this.getComponent(AnimatedForAvatarComponent.class);
		if (this.movementData.offset.len2() > 0) {
			if (anim != null) {
				anim.new_animation = avatarAnim.walk_anim;
			}
			footstepTimer += Gdx.graphics.getDeltaTime();
			if (footstepTimer > 0.45f) {
				footstepTimer -= 0.45f;
				Game.audio.play("step");
			}
		} else {
			if (anim != null) {
				anim.new_animation = avatarAnim.idle_anim;
			}
		}
	}

	public void renderUI(SpriteBatch batch, BitmapFont font) {
		/*if (interactTarget != null) {
			String str = interactTarget.getInteractText(this);
			int w2 = str.length() * 8;
			font.setColor(1,1,1,1);
			font.draw(batch, str, Gdx.graphics.getWidth() / 2 - w2, Gdx.graphics.getHeight() / 2 + 50/8);
		}*/

		/*for (int i = 0; i < inventory.keys; i++) {
			batch.draw(Game.art.items[0][0], 10 + i*50, Gdx.graphics.getHeight()-40, 48, 48);
		}*/

		//int sx = Gdx.graphics.getWidth()/2 - lives*18;
		/*int sx = (int)this.viewportData.viewPos.x + (this.viewportData.viewPos.width/2) - (lives*18);
		for (int i = 0; i < lives; i++) {
			//batch.draw(heart, sx + i*36, Gdx.graphics.getHeight()-40, 32, 32);
			batch.draw(heart, sx + i*36, this.viewportData.viewPos.y-40, 32, 32);
		}

		if (hurtTimer > 0 && (int)(hurtTimer*5)%2 == 0) {
			batch.setColor(1,1,1,.25f);
			//batch.draw(hurtTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch.draw(hurtTexture, this.viewportData.viewPos.x, this.viewportData.viewPos.y, this.viewportData.viewPos.width, this.viewportData.viewPos.height);
			batch.setColor(1,1,1,1);
		}*/

	}


}
