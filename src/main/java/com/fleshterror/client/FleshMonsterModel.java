package com.fleshterror.client;

import com.fleshterror.entity.FleshMonsterEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A lumpy, amorphous blob body held up by four multi-jointed leg-tentacles that plant on the
 * ground, plus four much longer arm-tentacles that stretch out, curl, and grip blocks. Every
 * tentacle ends in a small tapered "grip" segment. The whole thing is scaled again per
 * growth-stage by FleshMonsterRenderer.
 */
public class FleshMonsterModel extends HierarchicalModel<FleshMonsterEntity> {

    private static final int LEG_COUNT = 4;
    private static final int ARM_COUNT = 4;
    // Which arm indices count as the "front" pair that visibly reaches out to grab things.
    private static final int FRONT_ARM_A = 0;
    private static final int FRONT_ARM_B = 1;

    private final ModelPart root;
    private final ModelPart body;

    private final ModelPart[] legBase = new ModelPart[LEG_COUNT];
    private final ModelPart[] legMid = new ModelPart[LEG_COUNT];
    private final ModelPart[] legTip = new ModelPart[LEG_COUNT];

    private final ModelPart[] armBase = new ModelPart[ARM_COUNT];
    private final ModelPart[] armMid = new ModelPart[ARM_COUNT];
    private final ModelPart[] armTip = new ModelPart[ARM_COUNT];
    private final ModelPart[] armGrip = new ModelPart[ARM_COUNT];

    public FleshMonsterModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < LEG_COUNT; i++) {
            ModelPart base = root.getChild("leg_base_" + i);
            ModelPart mid = base.getChild("leg_mid_" + i);
            ModelPart tip = mid.getChild("leg_tip_" + i);
            legBase[i] = base;
            legMid[i] = mid;
            legTip[i] = tip;
        }
        for (int i = 0; i < ARM_COUNT; i++) {
            ModelPart base = root.getChild("arm_base_" + i);
            ModelPart mid = base.getChild("arm_mid_" + i);
            ModelPart tip = mid.getChild("arm_tip_" + i);
            ModelPart grip = tip.getChild("arm_grip_" + i);
            armBase[i] = base;
            armMid[i] = mid;
            armTip[i] = tip;
            armGrip[i] = grip;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // ---- Blobby body: several overlapping lumps instead of one clean cube ----
        CubeListBuilder bodyCubes = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -14.0F, -7.0F, 14.0F, 14.0F, 14.0F)   // core mass
                .texOffs(0, 0).addBox(-4.0F, -18.0F, -3.0F, 9.0F, 9.0F, 8.0F)      // upper lump
                .texOffs(0, 0).addBox(-3.0F, -3.0F, -4.0F, 7.0F, 6.0F, 7.0F)       // underbelly bulge
                .texOffs(0, 0).addBox(4.0F, -11.0F, -5.0F, 6.0F, 9.0F, 6.0F)       // right-side lump
                .texOffs(0, 0).addBox(-9.0F, -10.0F, -2.0F, 6.0F, 8.0F, 6.0F)      // left-side lump
                .texOffs(0, 0).addBox(-4.0F, -13.0F, 4.0F, 8.0F, 8.0F, 5.0F);      // rear lump

        parts.addOrReplaceChild("body", bodyCubes, PartPose.offset(0.0F, 18.0F, 0.0F));

        // ---- Legs: three joints each, rest pose lands exactly on the ground (y=24) ----
        double[] legX = {-6, 6, -6, 6};
        double[] legZ = {-6, -6, 6, 6};
        for (int i = 0; i < LEG_COUNT; i++) {
            float x = (float) legX[i];
            float z = (float) legZ[i];
            float leanX = z < 0 ? -0.18F : 0.18F;
            float leanZ = x < 0 ? -0.18F : 0.18F;
            int texX = i * 16;

            // Pivot at y=15, three 3-unit segments (9 total) reach exactly to y=24 (ground).
            PartDefinition base = parts.addOrReplaceChild("leg_base_" + i,
                    CubeListBuilder.create().texOffs(texX, 32).addBox(-1.6F, 0.0F, -1.6F, 3.2F, 3.0F, 3.2F),
                    PartPose.offsetAndRotation(x, 15.0F, z, leanX, 0.0F, leanZ));

            PartDefinition mid = base.addOrReplaceChild("leg_mid_" + i,
                    CubeListBuilder.create().texOffs(texX, 46).addBox(-1.3F, 0.0F, -1.3F, 2.6F, 3.0F, 2.6F),
                    PartPose.offset(0.0F, 3.0F, 0.0F));

            mid.addOrReplaceChild("leg_tip_" + i,
                    CubeListBuilder.create().texOffs(texX, 60).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.offset(0.0F, 3.0F, 0.0F));
        }

        // ---- Arms: long, four-jointed tentacles that reach out and curl to grip blocks ----
        double[] armAngleDeg = {35, -35, 145, -145}; // front pair (0,1) does the visible grabbing
        for (int i = 0; i < ARM_COUNT; i++) {
            double angle = Math.toRadians(armAngleDeg[i]);
            float x = (float) (Math.sin(angle) * 6.0);
            float z = (float) (Math.cos(angle) * 6.0);
            float yaw = (float) angle;
            int texX = i * 16;

            PartDefinition base = parts.addOrReplaceChild("arm_base_" + i,
                    CubeListBuilder.create().texOffs(texX, 74).addBox(-1.8F, 0.0F, -1.8F, 3.6F, 8.0F, 3.6F),
                    PartPose.offsetAndRotation(x, 4.0F, z, 1.15F, yaw, 0.0F));

            PartDefinition mid = base.addOrReplaceChild("arm_mid_" + i,
                    CubeListBuilder.create().texOffs(texX, 88).addBox(-1.4F, 0.0F, -1.4F, 2.8F, 8.0F, 2.8F),
                    PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.35F, 0.0F, 0.0F));

            PartDefinition tip = mid.addOrReplaceChild("arm_tip_" + i,
                    CubeListBuilder.create().texOffs(texX, 102).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.45F, 0.0F, 0.0F));

            tip.addOrReplaceChild("arm_grip_" + i,
                    CubeListBuilder.create().texOffs(texX, 116).addBox(-0.6F, 0.0F, -0.6F, 1.2F, 3.0F, 1.2F),
                    PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.6F, 0.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FleshMonsterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // Legs: alternating trot gait driven by actual movement, so it doesn't "moonwalk."
        float[] legPhase = {0.0F, (float) Math.PI, (float) Math.PI, 0.0F};
        for (int i = 0; i < LEG_COUNT; i++) {
            float swing = Mth.cos(limbSwing * 0.6662F + legPhase[i]) * 0.9F * limbSwingAmount;
            legBase[i].xRot += swing;
            legMid[i].xRot += swing * 0.6F;
            legTip[i].xRot += Math.abs(swing) * 0.5F;
        }

        boolean reaching = entity.isReaching();
        float t = ageInTicks * 0.06F;

        for (int i = 0; i < ARM_COUNT; i++) {
            float phase = i * ((float) Math.PI * 2 / ARM_COUNT);
            boolean isFrontArm = (i == FRONT_ARM_A || i == FRONT_ARM_B);

            if (reaching && isFrontArm) {
                // Reach forward and down toward the target, then curl the tip/grip inward
                // like it's wrapping around a block - a clear "grab" pose, not just a wiggle.
                float reachIn = Mth.sin(t * 2.2F + phase) * 0.15F;
                armBase[i].xRot += -0.55F + reachIn;
                armMid[i].xRot += -0.35F + reachIn * 0.8F;
                armTip[i].xRot += 0.55F + Mth.sin(t * 3.0F + phase) * 0.2F;   // curl inward
                armGrip[i].xRot += 0.9F + Mth.sin(t * 4.0F + phase) * 0.35F; // pincer open/close
            } else {
                // Idle ambient sway - slow, organic, reads as "alive" rather than attacking.
                float sway = Mth.sin(t + phase) * 0.22F;
                armBase[i].yRot += sway * 0.5F;
                armBase[i].xRot += Mth.cos(t * 0.7F + phase) * 0.08F;
                armMid[i].xRot += sway * 0.5F;
                armTip[i].xRot += sway * 0.7F;
                armGrip[i].xRot += Mth.sin(t * 1.5F + phase) * 0.25F;
            }
        }

        // Subtle body breathing pulse.
        float breathe = Mth.sin(ageInTicks * 0.05F) * 0.4F;
        body.y = 18.0F + breathe;
    }
}
