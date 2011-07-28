package rs2;


public class ItemDef
{

    public static void clearCache()
    {
        memCache2 = null;
        memCache1 = null;
        streamIndices = null;
        cache = null;
        stream = null;
    }

    public boolean isDownloaded(int j)
    {
        int k = maleDialogue;
        int l = maleDialogueHat;
        if(j == 1)
        {
            k = femaleDialogue;
            l = femaleDialogueHat;
        }
        if(k == -1)
            return true;
        boolean flag = true;
        if(!Model.isCached(k))
            flag = false;
        if(l != -1 && !Model.isCached(l))
            flag = false;
        return flag;
    }

    public static void unpackConfig(JagexArchive jagexArchive)
    {
        stream = new Packet(jagexArchive.getDataForName("obj.dat"));
        Packet stream = new Packet(jagexArchive.getDataForName("obj.idx"));
        totalItems = stream.g2();
        streamIndices = new int[totalItems];
        int i = 2;
        for(int j = 0; j < totalItems; j++)
        {
            streamIndices[j] = i;
            i += stream.g2();
        }

        cache = new ItemDef[10];
        for(int k = 0; k < 10; k++)
            cache[k] = new ItemDef();

    }

    public Model getHeadModel(int gender)
    {
        int k = maleDialogue;
        int l = maleDialogueHat;
        if(gender == 1)
        {
            k = femaleDialogue;
            l = femaleDialogueHat;
        }
        if(k == -1)
            return null;
        Model model = Model.getModel(k);
        if(l != -1)
        {
            Model model_1 = Model.getModel(l);
            Model aclass30_sub2_sub4_sub6s[] = {
                    model, model_1
            };
            model = new Model(2, aclass30_sub2_sub4_sub6s);
        }
        if(originalModelColours != null)
        {
            for(int i1 = 0; i1 < originalModelColours.length; i1++)
                model.recolour(originalModelColours[i1], modifiedModelColours[i1]);

        }
        return model;
    }

    public boolean hasItemEquipped(int gender)
    {
        int primaryModel = maleEquip1;
        int secondaryModel = maleEquip2;
        int emblem = maleEmblem;
        if(gender == 1)
        {
            primaryModel = femaleEquip1;
            secondaryModel = femaleEquip2;
            emblem = femaleEmblem;
        }
        if(primaryModel == -1)
            return true;
        boolean isEquipted = true;
        if(!Model.isCached(primaryModel))
            isEquipted = false;
        if(secondaryModel != -1 && !Model.isCached(secondaryModel))
            isEquipted = false;
        if(emblem != -1 && !Model.isCached(emblem))
            isEquipted = false;
        return isEquipted;
    }

    public Model method196(int gender)
    {
        int equip1 = maleEquip1;
        int equip2 = maleEquip2;
        int emblem = maleEmblem;
        if(gender == 1)
        {
            equip1 = femaleEquip1;
            equip2 = femaleEquip2;
            emblem = femaleEmblem;
        }
        if(equip1 == -1)
            return null;
        Model model = Model.getModel(equip1);
        if(equip2 != -1)
            if(emblem != -1)
            {
                Model model_1 = Model.getModel(equip2);
                Model model_3 = Model.getModel(emblem);
                Model aclass30_sub2_sub4_sub6_1s[] = {
                        model, model_1, model_3
                };
                model = new Model(3, aclass30_sub2_sub4_sub6_1s);
            } else
            {
                Model model_2 = Model.getModel(equip2);
                Model aclass30_sub2_sub4_sub6s[] = {
                        model, model_2
                };
                model = new Model(2, aclass30_sub2_sub4_sub6s);
            }
        if(gender == 0 && maleYOffset != 0)
            model.translate(0, maleYOffset, 0);
        if(gender == 1 && femaleYOffset != 0)
            model.translate(0, femaleYOffset, 0);
        if(originalModelColours != null)
        {
            for(int i1 = 0; i1 < originalModelColours.length; i1++)
                model.recolour(originalModelColours[i1], modifiedModelColours[i1]);

        }
        return model;
    }

    private void setDefaults()
    {
        modelID = 0;
        name = null;
        description = null;
        originalModelColours = null;
        modifiedModelColours = null;
        modelZoom = 2000;
        sprite_rotation_scale = 0;
        modelRotation2 = 0;
        anInt204 = 0;
        modelOffset1 = 0;
        modelOffset2 = 0;
        stackable = false;
        value = 1;
        membersObject = false;
        groundActions = null;
        actions = null;
        maleEquip1 = -1;
        maleEquip2 = -1;
        maleYOffset = 0;
        femaleEquip1 = -1;
        femaleEquip2 = -1;
        femaleYOffset = 0;
        maleEmblem = -1;
        femaleEmblem = -1;
        maleDialogue = -1;
        maleDialogueHat = -1;
        femaleDialogue = -1;
        femaleDialogueHat = -1;
        stackIDs = null;
        stackAmounts = null;
        certID = -1;
        certTemplateID = -1;
        modelSizeX = 128;
        modelSizeY = 128;
        modelSizeZ = 128;
        lightModifier = 0;
        magMultiplyer = 0;
        team = 0;
    }

    public static ItemDef forID(int i)
    {
        for(int j = 0; j < 10; j++)
            if(cache[j].id == i)
                return cache[j];

        cacheIndex = (cacheIndex + 1) % 10;
        ItemDef itemDef = cache[cacheIndex];
        stream.pos = streamIndices[i];
        itemDef.id = i;
        itemDef.setDefaults();
        itemDef.readValues(stream);
        if(itemDef.certTemplateID != -1)
            itemDef.toNote();
        if(!isMembers && itemDef.membersObject)
        {
            itemDef.name = "Members Object";
            itemDef.description = "Login to a members' server to use this object.".getBytes();
            itemDef.groundActions = null;
            itemDef.actions = null;
            itemDef.team = 0;
        }
        return itemDef;
    }

    private void toNote()
    {
        ItemDef itemDef = forID(certTemplateID);
        modelID = itemDef.modelID;
        modelZoom = itemDef.modelZoom;
        sprite_rotation_scale = itemDef.sprite_rotation_scale;
        modelRotation2 = itemDef.modelRotation2;

        anInt204 = itemDef.anInt204;
        modelOffset1 = itemDef.modelOffset1;
        modelOffset2 = itemDef.modelOffset2;
        originalModelColours = itemDef.originalModelColours;
        modifiedModelColours = itemDef.modifiedModelColours;
        ItemDef itemDef_1 = forID(certID);
        name = itemDef_1.name;
        membersObject = itemDef_1.membersObject;
        value = itemDef_1.value;
        String s = "a";
        char c = itemDef_1.name.charAt(0);
        if(c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U')
            s = "an";
        description = ("Swap this note at any bank for " + s + " " + itemDef_1.name + ".").getBytes();
        stackable = true;
    }

    public static RgbImage getSprite(int i, int j, int k)
    {
        if(k == 0)
        {
            RgbImage rgbImage = (RgbImage) memCache1.get(i);
            if(rgbImage != null && rgbImage.h2 != j && rgbImage.h2 != -1)
            {
                rgbImage.unlink();
                rgbImage = null;
            }
            if(rgbImage != null)
                return rgbImage;
        }
        ItemDef definition = forID(i);
        if(definition.stackIDs == null)
            j = -1;
        if(j > 1)
        {
            int i1 = -1;
            for(int j1 = 0; j1 < 10; j1++)
                if(j >= definition.stackAmounts[j1] && definition.stackAmounts[j1] != 0)
                    i1 = definition.stackIDs[j1];

            if(i1 != -1)
                definition = forID(i1);
        }
        Model model = definition.getModelForAmmount(1);
        if(model == null)
            return null;
        RgbImage rgbImage = null;
        if(definition.certTemplateID != -1)
        {
            rgbImage = getSprite(definition.certID, 10, -1);
            if(rgbImage == null)
                return null;
        }
        RgbImage sprite2 = new RgbImage(32, 32);
        int k1 = Rasterizer.centerX;
        int l1 = Rasterizer.centerY;
        int ai[] = Rasterizer.lineOffsets;
        int ai1[] = Graphics2D.pixels;
        int i2 = Graphics2D.width;
        int j2 = Graphics2D.height;
        int k2 = Graphics2D.topX;
        int l2 = Graphics2D.viewport_w;
        int i3 = Graphics2D.topY;
        int j3 = Graphics2D.viewport_h;
        Rasterizer.notTextured = false;
        Graphics2D.setTarget(32, 32, sprite2.myPixels);
        Graphics2D.fillRect(0, 0, 32, 32, 0);
        Rasterizer.setDefaultBounds();
        int k3 = definition.modelZoom;
        if(k == -1)
            k3 = (int)((double)k3 * 1.5D);
        if(k > 0)
            k3 = (int)((double)k3 * 1.04D);
        int l3 = Rasterizer.SINE[definition.sprite_rotation_scale] * k3 >> 16;
        int i4 = Rasterizer.COSINE[definition.sprite_rotation_scale] * k3 >> 16;
        model.rendersingle(definition.modelRotation2, definition.anInt204, definition.sprite_rotation_scale, definition.modelOffset1, l3 + model.modelHeight / 2 + definition.modelOffset2, i4 + definition.modelOffset2);
        for(int i5 = 31; i5 >= 0; i5--)
        {
            for(int j4 = 31; j4 >= 0; j4--)
                if(sprite2.myPixels[i5 + j4 * 32] == 0)
                    if(i5 > 0 && sprite2.myPixels[(i5 - 1) + j4 * 32] > 1)
                        sprite2.myPixels[i5 + j4 * 32] = 1;
                    else
                    if(j4 > 0 && sprite2.myPixels[i5 + (j4 - 1) * 32] > 1)
                        sprite2.myPixels[i5 + j4 * 32] = 1;
                    else
                    if(i5 < 31 && sprite2.myPixels[i5 + 1 + j4 * 32] > 1)
                        sprite2.myPixels[i5 + j4 * 32] = 1;
                    else
                    if(j4 < 31 && sprite2.myPixels[i5 + (j4 + 1) * 32] > 1)
                        sprite2.myPixels[i5 + j4 * 32] = 1;

        }

        if(k > 0)
        {
            for(int j5 = 31; j5 >= 0; j5--)
            {
                for(int k4 = 31; k4 >= 0; k4--)
                    if(sprite2.myPixels[j5 + k4 * 32] == 0)
                        if(j5 > 0 && sprite2.myPixels[(j5 - 1) + k4 * 32] == 1)
                            sprite2.myPixels[j5 + k4 * 32] = k;
                        else
                        if(k4 > 0 && sprite2.myPixels[j5 + (k4 - 1) * 32] == 1)
                            sprite2.myPixels[j5 + k4 * 32] = k;
                        else
                        if(j5 < 31 && sprite2.myPixels[j5 + 1 + k4 * 32] == 1)
                            sprite2.myPixels[j5 + k4 * 32] = k;
                        else
                        if(k4 < 31 && sprite2.myPixels[j5 + (k4 + 1) * 32] == 1)
                            sprite2.myPixels[j5 + k4 * 32] = k;

            }

        } else
        if(k == 0)
        {
            for(int k5 = 31; k5 >= 0; k5--)
            {
                for(int l4 = 31; l4 >= 0; l4--)
                    if(sprite2.myPixels[k5 + l4 * 32] == 0 && k5 > 0 && l4 > 0 && sprite2.myPixels[(k5 - 1) + (l4 - 1) * 32] > 0)
                        sprite2.myPixels[k5 + l4 * 32] = 0x302020;

            }

        }
        if(definition.certTemplateID != -1)
        {
            int l5 = rgbImage.w2;
            int j6 = rgbImage.h2;
            rgbImage.w2 = 32;
            rgbImage.h2 = 32;
            rgbImage.drawSprite(0, 0);
            rgbImage.w2 = l5;
            rgbImage.h2 = j6;
        }
        if(k == 0)
            memCache1.put(sprite2, i);
        Graphics2D.setTarget(j2, i2, ai1);
        Graphics2D.setBounds(j3, k2, l2, i3);
        Rasterizer.centerX = k1;
        Rasterizer.centerY = l1;
        Rasterizer.lineOffsets = ai;
        Rasterizer.notTextured = true;
        if(definition.stackable)
            sprite2.w2 = 33;
        else
            sprite2.w2 = 32;
        sprite2.h2 = j;
        return sprite2;
    }

    public Model getModelForAmmount(int i)
    {
        if(stackIDs != null && i > 1)
        {
            int j = -1;
            for(int k = 0; k < 10; k++)
                if(i >= stackAmounts[k] && stackAmounts[k] != 0)
                    j = stackIDs[k];

            if(j != -1)
                return forID(j).getModelForAmmount(1);
        }
        Model model = (Model) memCache2.get(id);
        if(model != null)
            return model;
        model = Model.getModel(modelID);
        if(model == null)
            return null;
        if(modelSizeX != 128 || modelSizeY != 128 || modelSizeZ != 128)
            model.scaleT(modelSizeX, modelSizeZ, modelSizeY);
        if(originalModelColours != null)
        {
            for(int colourPtr = 0; colourPtr < originalModelColours.length; colourPtr++)
                model.recolour(originalModelColours[colourPtr], modifiedModelColours[colourPtr]);

        }
        model.light(64 + lightModifier, 768 + magMultiplyer, -50, -10, -50, true);
        model.aBoolean1659 = true;
        memCache2.put(model, id);
        return model;
    }

    public Model getInventoryModel(int i)
    {
        if(stackIDs != null && i > 1)
        {
            int j = -1;
            for(int k = 0; k < 10; k++)
                if(i >= stackAmounts[k] && stackAmounts[k] != 0)
                    j = stackIDs[k];

            if(j != -1)
                return forID(j).getInventoryModel(1);
        }
        Model model = Model.getModel(modelID);
        if(model == null)
            return null;
        if(originalModelColours != null)
        {
            for(int colourPtr = 0; colourPtr < originalModelColours.length; colourPtr++)
                model.recolour(originalModelColours[colourPtr], modifiedModelColours[colourPtr]);

        }
        return model;
    }

    private void readValues(Packet stream)
    {
        do
        {
            int i = stream.g1();
            if(i == 0)
                return;
            if(i == 1)
                modelID = stream.g2();
            else
            if(i == 2)
                name = stream.gstr();
            else
            if(i == 3)
                description = stream.gstrbyte();
            else
            if(i == 4)
                modelZoom = stream.g2();
            else
            if(i == 5)
                sprite_rotation_scale = stream.g2();
            else
            if(i == 6)
                modelRotation2 = stream.g2();
            else
            if(i == 7)
            {
                modelOffset1 = stream.g2();
                if(modelOffset1 > 32767)
                    modelOffset1 -= 0x10000;
            } else
            if(i == 8)
            {
                modelOffset2 = stream.g2();
                if(modelOffset2 > 32767)
                    modelOffset2 -= 0x10000;
            } else
            if(i == 10)
                stream.g2();
            else
            if(i == 11)
                stackable = true;
            else
            if(i == 12)
                value = stream.g4();
            else
            if(i == 16)
                membersObject = true;
            else
            if(i == 23)
            {
                maleEquip1 = stream.g2();
                maleYOffset = stream.g1b();
            } else
            if(i == 24)
                maleEquip2 = stream.g2();
            else
            if(i == 25)
            {
                femaleEquip1 = stream.g2();
                femaleYOffset = stream.g1b();
            } else
            if(i == 26)
                femaleEquip2 = stream.g2();
            else
            if(i >= 30 && i < 35)
            {
                if(groundActions == null)
                    groundActions = new String[5];
                groundActions[i - 30] = stream.gstr();
                if(groundActions[i - 30].equalsIgnoreCase("hidden"))
                    groundActions[i - 30] = null;
            } else
            if(i >= 35 && i < 40)
            {
                if(actions == null)
                    actions = new String[5];
                actions[i - 35] = stream.gstr();
            } else
            if(i == 40)
            {
                int j = stream.g1();
                originalModelColours = new int[j];
                modifiedModelColours = new int[j];
                for(int k = 0; k < j; k++)
                {
                    originalModelColours[k] = stream.g2();
                    modifiedModelColours[k] = stream.g2();
                }

            } else
            if(i == 78)
                maleEmblem = stream.g2();
            else
            if(i == 79)
                femaleEmblem = stream.g2();
            else
            if(i == 90)
                maleDialogue = stream.g2();
            else
            if(i == 91)
                femaleDialogue = stream.g2();
            else
            if(i == 92)
                maleDialogueHat = stream.g2();
            else
            if(i == 93)
                femaleDialogueHat = stream.g2();
            else
            if(i == 95)
                anInt204 = stream.g2();
            else
            if(i == 97)
                certID = stream.g2();
            else
            if(i == 98)
                certTemplateID = stream.g2();
            else
            if(i >= 100 && i < 110)
            {
                if(stackIDs == null)
                {
                    stackIDs = new int[10];
                    stackAmounts = new int[10];
                }
                stackIDs[i - 100] = stream.g2();
                stackAmounts[i - 100] = stream.g2();
            } else
            if(i == 110)
                modelSizeX = stream.g2();
            else
            if(i == 111)
                modelSizeY = stream.g2();
            else
            if(i == 112)
                modelSizeZ = stream.g2();
            else
            if(i == 113)
                lightModifier = stream.g1b();
            else
            if(i == 114)
                magMultiplyer = stream.g1b() * 5;
            else
            if(i == 115)
                team = stream.g1();
        } while(true);
    }

    private ItemDef()
    {
        id = -1;
    }

    private byte femaleYOffset;
    public int value;
    private int[] originalModelColours;
    public int id;
    public static MemCache memCache1 = new MemCache(100);
    public static MemCache memCache2 = new MemCache(50);
    private int[] modifiedModelColours;
    public boolean membersObject;
    private int femaleEmblem;
    private int certTemplateID;
    private int femaleEquip2;
    private int maleEquip1;
    private int maleDialogueHat;
    private int modelSizeX;
    public String groundActions[];
    private int modelOffset1;
    public String name;
    private static ItemDef[] cache;
    private int femaleDialogueHat;
    private int modelID;
    private int maleDialogue;
    public boolean stackable;
    public byte description[];
    private int certID;
    private static int cacheIndex;
    public int modelZoom;
    public static boolean isMembers = true;
    private static Packet stream;
    private int magMultiplyer;
    private int maleEmblem;
    private int maleEquip2;
    public String actions[];
    public int sprite_rotation_scale;
    private int modelSizeZ;
    private int modelSizeY;
    private int[] stackIDs;
    private int modelOffset2;
    private static int[] streamIndices;
    private int lightModifier;
    private int femaleDialogue;
    public int modelRotation2;
    private int femaleEquip1;
    private int[] stackAmounts;
    public int team;
    public static int totalItems;
    private int anInt204;
    private byte maleYOffset;

}