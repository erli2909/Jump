package com.erli.jump;

import android.content.ContentValues;
import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;

import org.andengine.engine.camera.SmoothCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.scene.background.RepeatingSpriteBackground;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.SAXUtils;
import org.andengine.util.color.Color;
import org.andengine.util.debug.Debug;
import org.andengine.util.level.IEntityLoader;
import org.andengine.util.level.LevelLoader;
import org.andengine.util.level.constants.LevelConstants;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;


public class GameActivity extends SimpleBaseGameActivity implements IAccelerationListener, IUpdateHandler {

    private SmoothCamera camera;
    private static final int CAMERA_WIDTH = 1080; //36
    private static final int CAMERA_HEIGHT = 1920; //64
    private static final int LEVEL = 2;
    private static final int PIXEL = 30;
    private static final int SIDE_SPEED = 10;
    private static final int JUMP_SPEED = -20;
    private static final int MAX_JUMP = CAMERA_WIDTH / 2;
    private static final int GRAVITY = 50;


    private static final String TAG_CLOUD = "cloud";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_TYPE = "type";
    private static final String TAG_ATT = "att";
    private static final String TAG_TYPE_NORMAL = "normal";
    private static final String TAG_TYPE_ONE = "one";

    private static final FixtureDef FIXTURE_DEF = PhysicsFactory.createFixtureDef(1, 0f, 0f);

    Color colorRed, colorIndigo, colorIndigoPressed, colorTeal;

    public float lastCloud = CAMERA_HEIGHT / 2;
    public float lastCrash = CAMERA_HEIGHT / 2;

    public Scene scene;
    private BitmapTextureAtlas mTexture;
    public ITextureRegion mTextureCloud, mTextureCloudOne, mTextureGrass;
    public RepeatingSpriteBackground background;
    public ITiledTextureRegion mTextureDuck;
    public Body duckBody, duckLineBody;
    public AnimatedSprite duckSprite;
    public Rectangle duckLine;
    public ArrayList<Line> cloudArray = new ArrayList<Line>();
    public ArrayList<ArrayList<ContentValues>> levelManagerArray = new ArrayList<ArrayList<ContentValues>>();

    private PhysicsWorld mPhysicsWorld;

    @Override
    public void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
        mTexture = new BitmapTextureAtlas(getTextureManager(), 1024, 1024, TextureOptions.DEFAULT);
        mTextureDuck = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mTexture, this, "duck.png", 0, 0, 2, 1);
        mTextureCloud = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mTexture, this, "cloud.png", 0, 256);
        mTextureCloudOne = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mTexture, this, "cloud_2.png", 256, 256);
        mTextureGrass = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mTexture, this, "grass.png", 0, 512);
        //background = new RepeatingSpriteBackground(CAMERA_WIDTH, CAMERA_HEIGHT, getTextureManager(), AssetBitmapTextureAtlasSource.create(this.getAssets(), "gfx/block.png"), getVertexBufferObjectManager());
        mTexture.load();
    }

    @Override
    protected Scene onCreateScene() {
        mEngine.registerUpdateHandler(new FPSLogger());
        scene = new Scene();
        scene.setBackground(new Background(0.9f, 0.95f, 1));
        //scene.setBackground(background);

        mPhysicsWorld = new PhysicsWorld(new Vector2(0, 0), true);
        final Vector2 gravity = new Vector2(0, GRAVITY);
        mPhysicsWorld.setGravity(gravity);

        for (int i = 0; i < LEVEL; i++) {
            final ArrayList<ContentValues> levelArray = new ArrayList<ContentValues>();
            final LevelLoader levelLoader = new LevelLoader();
            levelLoader.setAssetBasePath("level/");
            levelLoader.registerEntityLoader(LevelConstants.TAG_LEVEL, new IEntityLoader() {
                @Override
                public IEntity onLoadEntity(final String pEntityName, final Attributes pAttributes) {
                    return scene;
                }
            });
            levelLoader.registerEntityLoader(TAG_CLOUD, new IEntityLoader() {
                @Override
                public IEntity onLoadEntity(final String pEntityName, final Attributes pAttributes) {
                    final int x = SAXUtils.getIntAttributeOrThrow(pAttributes, TAG_X) * PIXEL;
                    final int y = SAXUtils.getIntAttributeOrThrow(pAttributes, TAG_Y) * PIXEL;
                    final String type = SAXUtils.getAttributeOrThrow(pAttributes, TAG_TYPE);
                    final String att = SAXUtils.getAttributeOrThrow(pAttributes, TAG_ATT);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put("x", x);
                    contentValues.put("y", y);
                    contentValues.put("type", type);
                    contentValues.put("att", att);
                    levelArray.add(contentValues);

                    return null;
                }
            });
            try {
                levelLoader.loadLevelFromAsset(this.getAssets(), "level" + i + ".lvl");
            } catch (final IOException e) {
                Debug.e(e);
            }
            levelManagerArray.add(levelArray);
        }

        addClouds(0);


        duckSprite = new AnimatedSprite(CAMERA_WIDTH / 2 - 128, PIXEL * 20, mTextureDuck, getVertexBufferObjectManager());
        duckBody = PhysicsFactory.createBoxBody(mPhysicsWorld, duckSprite, BodyDef.BodyType.DynamicBody, FIXTURE_DEF);
        mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(duckSprite, duckBody, true, false));
        duckLine = new Rectangle(duckSprite.getX() + duckSprite.getWidth() / 2 - 40, duckSprite.getY() + duckSprite.getHeight() + 1, 40, 30, getVertexBufferObjectManager());
        duckLine.setColor(0, 0, 1);
        //duckLine.setAlpha(0);
        duckLineBody = PhysicsFactory.createBoxBody(mPhysicsWorld, duckLine, BodyDef.BodyType.DynamicBody, FIXTURE_DEF);
        mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(duckLine, duckLineBody, true, false));

        scene.attachChild(duckSprite);
        scene.attachChild(duckLine);

        final WeldJointDef weldJointDef = new WeldJointDef();
        weldJointDef.initialize(duckBody, duckLineBody, duckBody.getLocalCenter());
        mPhysicsWorld.createJoint(weldJointDef);
        //sprite.animate(200);


        HUD hud = new HUD();
        camera.setHUD(hud);
        for (int i = 0; i < 10; i++) {
            final Sprite sprite = new Sprite(i * 128, 1822, mTextureGrass, getVertexBufferObjectManager());
            hud.attachChild(sprite);
        }

        scene.registerUpdateHandler(mPhysicsWorld);
        return scene;
    }

    private void addClouds(int i) {
        for (ContentValues cv : levelManagerArray.get(i)) {
            if (cv.get("type").equals(TAG_TYPE_NORMAL)) {
                addCloud(cv.getAsInteger("x"), cv.getAsInteger("y"), mTextureCloud);
            } else if (cv.get("type").equals(TAG_TYPE_ONE)) {
                addCloud(cv.getAsInteger("x"), cv.getAsInteger("y"), mTextureCloudOne);
            }
        }
        lastCloud = cloudArray.get(cloudArray.size() - 1).getY1();
    }

    private Sprite addCloud(int x, int y, ITextureRegion textureRegion) {
        y += lastCloud;
        final Sprite sprite = new Sprite(x, y, textureRegion, getVertexBufferObjectManager());
        final Line line = new Line(x + 10, y + 80, x + sprite.getWidth() - 10, y + 80, getVertexBufferObjectManager());
        line.setColor(1, 0, 0);
        //line.setAlpha(0);
        cloudArray.add(line);
        scene.attachChild(sprite);
        scene.attachChild(line);
        return sprite;
    }

    @Override
    public EngineOptions onCreateEngineOptions() {
        camera = new SmoothCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT, 0, 1000, 1);

        colorRed = new Color((float) 244 / 255, (float) 67 / 255, (float) 54 / 255);
        colorIndigo = new Color((float) 63 / 255, (float) 81 / 255, (float) 181 / 255);
        colorIndigoPressed = new Color((float) 121 / 255, (float) 134 / 255, (float) 203 / 255);
        colorTeal = new Color(0, (float) 150 / 255, (float) 136 / 255);

        EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED,
                new FillResolutionPolicy(), camera);
        return engineOptions;
    }

    @Override
    public void onAccelerationAccuracyChanged(AccelerationData pAccelerationData) {

    }

    @Override
    public void onAccelerationChanged(final AccelerationData pAccelerationData) {
        duckBody.setLinearVelocity(pAccelerationData.getX() * SIDE_SPEED, duckBody.getLinearVelocity().y);

        if (duckBody.getPosition().x > CAMERA_WIDTH / 32) {
            duckBody.setTransform(duckBody.getPosition().x - (CAMERA_WIDTH / 32), duckBody.getPosition().y, 0);
        } else if (duckBody.getPosition().x < 0) {
            duckBody.setTransform(duckBody.getPosition().x + (CAMERA_WIDTH / 32), duckBody.getPosition().y, 0);
        }
    }

    @Override
    public void onResumeGame() {
        super.onResumeGame();
        this.enableAccelerationSensor(this);
    }

    @Override
    public void onPauseGame() {
        super.onPauseGame();

        this.disableAccelerationSensor();
    }

    @Override
    public void onUpdate(float pSecondsElapsed) {
        Log.i("Speed", String.valueOf(duckBody.getLinearVelocity().y));

        //Clouds
        for (Line line : cloudArray) {
            //Collision
            if (duckLine.collidesWith(line)) {
                duckBody.setLinearVelocity(duckBody.getLinearVelocity().x, JUMP_SPEED);
                lastCrash = duckBody.getLocalCenter().y * 32;
            }

            if (line.getY1() < camera.getCenterY() - CAMERA_HEIGHT) {
                cloudArray.remove(line);
                break;
            }
        }

        //Add Clouds
        if (cloudArray.size() < 10) {
            Random random = new Random();
            addClouds(random.nextInt(LEVEL));
        }

        //Camera follow body
        if (duckBody.getPosition().y * 32 > camera.getCenterY() + 300) {
            camera.setCenter(CAMERA_WIDTH / 2, duckBody.getPosition().y * 32);
        }

        //Game Over
        if (duckBody.getLocalCenter().y * 32 > lastCrash + MAX_JUMP) {
            cloudArray.clear();
            Log.i("Game", "Game Over!");
        }
    }

    @Override
    public void reset() {

    }
}

