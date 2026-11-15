package net.etylop.immersivefarming.client.renderer.entity.model;

import net.etylop.immersivefarming.entity.SowerEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/**
 * A seed drill trailing behind the cart: two lift arms hinged on top of the frame carry a hopper
 * (seeds in the front trough, fertilizer in the back one) above three disc coulters, one per sown row.
 */
public final class SowerModel extends CartModel<SowerEntity> {
    private static final float ARM_LENGTH = 20.0F;
    /** Height of the arm pivot above the axle: the offsets of "parts" and "lift_arms" */
    private static final float PIVOT_HEIGHT = 13.0F;
    /** Height of the hopper hinge above the axle when the discs touch the ground, and when they are lifted */
    private static final float LOWERED_HINGE = 4.0F;
    private static final float RAISED_HINGE = 9.5F;
    /** How far the content of the hopper sinks between full and nearly empty */
    private static final float FILL_TRAVEL = 4.0F;
    /** Wheel radius / disc radius: the discs roll on the ground as the wheels do */
    private static final float DISC_GEARING = 11.0F / 3.5F;

    private final ModelPart liftArms;
    private final ModelPart drill;
    private final ModelPart seedFill;
    private final ModelPart compostFill;
    private final ModelPart[] discs = new ModelPart[3];

    public SowerModel(final ModelPart root) {
        super(root);
        this.liftArms = root.getChild("body").getChild("parts").getChild("lift_arms");
        this.drill = this.liftArms.getChild("drill");
        this.seedFill = this.drill.getChild("seed_fill");
        this.compostFill = this.drill.getChild("compost_fill");
        for (int i = 0; i < this.discs.length; i++) {
            this.discs[i] = this.drill.getChild("disc_" + i);
        }
    }

    @Override
    public void setupAnim(final SowerEntity entity, final float delta, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float pitch) {
        super.setupAnim(entity, delta, limbSwingAmount, ageInTicks, netHeadYaw, pitch);
        final boolean lowered = entity.getPlowing();
        final float pitchRad = (float) Math.toRadians(pitch);

        // The hopper stays level and at the same height however the cart is tilted (it tilts a lot when unhitched):
        // the arms swing by whatever it takes to cover the height between their pivot and the hopper hinge
        final float drop = PIVOT_HEIGHT * Mth.cos(pitchRad) - (lowered ? LOWERED_HINGE : RAISED_HINGE);
        final float armAngle = (float) Math.acos(Mth.clamp(drop / ARM_LENGTH, -1.0F, 1.0F));
        this.liftArms.xRot = armAngle - pitchRad;
        this.drill.xRot = -armAngle;

        setFill(this.seedFill, entity.getSeedFill());
        setFill(this.compostFill, entity.getFertilizerFill());

        this.discs[0].xRot = lowered ? this.rightWheel.xRot * DISC_GEARING : 0.0F;
        this.discs[1].xRot = lowered ? (this.rightWheel.xRot + this.leftWheel.xRot) * 0.5F * DISC_GEARING : 0.0F;
        this.discs[2].xRot = lowered ? this.leftWheel.xRot * DISC_GEARING : 0.0F;
    }

    private static void setFill(final ModelPart content, final float fill) {
        content.visible = fill > 0.0F;
        content.y = FILL_TRAVEL * (1.0F - fill);
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition def = CartModel.createDefinition();

        final EasyMeshBuilder axis = new EasyMeshBuilder("axis", 0, 0);
        axis.addBox(-12.5F, -1.0F, -1.0F, 25, 2, 2);

        final EasyMeshBuilder[] triangle = new EasyMeshBuilder[3];
        triangle[0] = new EasyMeshBuilder("triangle_0", 0, 4);
        triangle[0].addBox(-7.5F, -9.0F, 0.0F, 15, 2, 2);

        triangle[1] = new EasyMeshBuilder("triangle_1", 0, 11);
        triangle[1].addBox(-5.0F, -9.0F, 0.5F, 2, 14, 2);
        triangle[1].zRot = -0.175F;

        triangle[2] = new EasyMeshBuilder("triangle_2", 0, 11);
        triangle[2].mirror(true);
        triangle[2].addBox(3.0F, -9.0F, 0.5F, 2, 14, 2);
        triangle[2].zRot = 0.175F;

        final EasyMeshBuilder shaft = new EasyMeshBuilder("shaft", 0, 8);
        shaft.zRot = -0.07F;
        shaft.addBox(0.0F, 0.0F, -8.0F, 20, 2, 1);
        shaft.addBox(0.0F, 0.0F, 7.0F, 20, 2, 1);

        final EasyMeshBuilder shaftConnector = new EasyMeshBuilder("shaftConnector", 0, 27);
        shaftConnector.zRot = -0.26F;
        shaftConnector.addBox(-16.0F, 0.0F, -8.0F, 16, 2, 1);
        shaftConnector.addBox(-16.0F, 0.0F, 7.0F, 16, 2, 1);

        final EasyMeshBuilder shafts = new EasyMeshBuilder("shafts");
        shafts.setRotationPoint(0.0F, 0.0F, -14.0F);
        shafts.yRot = (float) Math.PI / 2.0F;
        shafts.addChild(shaft);
        shafts.addChild(shaftConnector);

        // Two arms hinged on the ends of the top beam of the frame, running back and down to the hopper
        final EasyMeshBuilder liftArms = new EasyMeshBuilder("lift_arms", 56, 0);
        liftArms.setRotationPoint(0.0F, -8.0F, 1.0F);
        liftArms.addBox(-9.5F, -1.0F, -1.0F, 2, 22, 2);
        liftArms.addBox(7.5F, -1.0F, -1.0F, 2, 22, 2);

        final EasyMeshBuilder armBrace = new EasyMeshBuilder("arm_brace", 0, 4);
        armBrace.addBox(-7.5F, 10.0F, -0.5F, 15, 2, 1);
        liftArms.addChild(armBrace);

        // Everything hanging from the arms. Y goes down and Z back from the hinge, the ground is at Y = 15 when sowing
        final EasyMeshBuilder drill = new EasyMeshBuilder("drill", 0, 107);
        drill.setRotationPoint(0.0F, ARM_LENGTH, 0.0F);
        drill.addBox(-10.0F, -0.5F, -0.5F, 20, 1, 1); // hinge rod
        drill.setTextureOffset(0, 64);
        drill.addBox(-22.0F, -2.0F, 0.5F, 44, 6, 1); // front wall
        drill.addBox(-22.0F, -2.0F, 10.5F, 44, 6, 1); // back wall
        drill.setTextureOffset(0, 109);
        drill.addBox(-22.0F, -2.0F, 1.5F, 1, 6, 9); // end walls
        drill.addBox(21.0F, -2.0F, 1.5F, 1, 6, 9);
        drill.setTextureOffset(0, 71);
        drill.addBox(-22.0F, 4.0F, 0.5F, 44, 1, 11); // floor
        drill.setTextureOffset(0, 83);
        drill.addBox(-20.0F, 5.0F, 3.5F, 40, 3, 5); // chute under the floor
        drill.setTextureOffset(0, 91);
        drill.addBox(-21.0F, -1.0F, 5.5F, 42, 5, 1); // divider between seeds and fertilizer

        // Content of the two troughs, drawn full: setupAnim sinks or hides them
        final EasyMeshBuilder seedFill = new EasyMeshBuilder("seed_fill", 0, 97);
        seedFill.addBox(-21.0F, -1.0F, 1.5F, 42, 1, 4);
        drill.addChild(seedFill);

        final EasyMeshBuilder compostFill = new EasyMeshBuilder("compost_fill", 0, 102);
        compostFill.addBox(-21.0F, -1.0F, 6.5F, 42, 1, 4);
        drill.addChild(compostFill);

        // One spout and one disc per sown row, a block apart
        for (int i = 0; i < 3; i++) {
            final float x = -16.0F + 16.0F * i;

            final EasyMeshBuilder spout = new EasyMeshBuilder("spout_" + i, 20, 109);
            spout.addBox(x - 1.0F, 8.0F, 5.0F, 2, 3, 2);
            drill.addChild(spout);

            // A 7 pixel wide disc, rounded with stacked boxes
            final EasyMeshBuilder disc = new EasyMeshBuilder("disc_" + i, 28, 109);
            disc.setRotationPoint(x, 12.0F, 6.0F);
            disc.addBox(-0.5F, -3.5F, -1.5F, 1, 7, 3);
            disc.setTextureOffset(36, 109);
            disc.addBox(-0.5F, -2.5F, -2.5F, 1, 5, 1);
            disc.addBox(-0.5F, -2.5F, 1.5F, 1, 5, 1);
            disc.setTextureOffset(40, 109);
            disc.addBox(-0.5F, -1.5F, -3.5F, 1, 3, 1);
            disc.addBox(-0.5F, -1.5F, 2.5F, 1, 3, 1);
            drill.addChild(disc);
        }
        liftArms.addChild(drill);

        final EasyMeshBuilder parts = new EasyMeshBuilder("parts");
        parts.setRotationPoint(0.0F, -5.0F, -1.0F);
        parts.addChild(shafts);
        parts.addChild(triangle[0]);
        parts.addChild(triangle[1]);
        parts.addChild(triangle[2]);
        parts.addChild(liftArms);

        final EasyMeshBuilder body = CartModel.createBody();
        body.addChild(axis);
        body.addChild(parts);
        body.build(def.getRoot());

        return LayerDefinition.create(def, 128, 128);
    }
}
