package com.mramadan.babybubbles.game

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.mramadan.babybubbles.R

enum class GameMode { COLORS, ANIMALS, VEHICLES, BIRDS, FRUITS, SEA, FLAGS }

/** How a character leaves after its bubble pops. */
enum class PopStyle { JUMP, DRIVE, RISE }

/**
 * One thing that can float inside a bubble. Names carry full diacritics so the
 * phone's voice pronounces them correctly (جَمَل, not جُمل).
 */
class Character(
    @DrawableRes val image: Int,
    /** Its own real recording, or 0 to use the mode's default pop sound. */
    @RawRes val sound: Int,
    val arabic: String,
    val english: String,
)

val GameMode.popStyle: PopStyle
    get() = when (this) {
        GameMode.VEHICLES -> PopStyle.DRIVE
        GameMode.FLAGS -> PopStyle.RISE
        else -> PopStyle.JUMP
    }

/** Flags are pictures with a 4:3 frame; everything else is square art. */
val GameMode.isFlag: Boolean get() = this == GameMode.FLAGS

@RawRes
fun GameMode.defaultPopSound(): Int = when (this) {
    GameMode.FRUITS -> R.raw.snd_crunch
    GameMode.SEA -> R.raw.snd_blub
    else -> R.raw.snd_bubbles
}

object Catalog {
    val animals = listOf(
        Character(R.drawable.animal_cat, R.raw.snd_cat, "قِطَّة", "Cat"),
        Character(R.drawable.animal_dog, R.raw.snd_dog, "كَلْب", "Dog"),
        Character(R.drawable.animal_cow, R.raw.snd_cow, "بَقَرَة", "Cow"),
        Character(R.drawable.animal_sheep, R.raw.snd_sheep, "خَرُوف", "Sheep"),
        Character(R.drawable.animal_duck, R.raw.snd_duck, "بَطَّة", "Duck"),
        Character(R.drawable.animal_chicken, R.raw.snd_chicken, "دَجَاجَة", "Chicken"),
        Character(R.drawable.animal_horse, R.raw.snd_horse, "حِصَان", "Horse"),
        Character(R.drawable.animal_lion, R.raw.snd_lion, "أَسَد", "Lion"),
        Character(R.drawable.animal_elephant, R.raw.snd_elephant, "فِيل", "Elephant"),
        Character(R.drawable.animal_frog, R.raw.snd_frog, "ضِفْدَع", "Frog"),
        Character(R.drawable.animal_camel, R.raw.snd_camel, "جَمَل", "Camel"),
        Character(R.drawable.animal_owl, R.raw.snd_owl, "بُومَة", "Owl"),
    )

    val birds = listOf(
        Character(R.drawable.bird_sparrow, R.raw.snd_sparrow, "عُصْفُور", "Sparrow"),
        Character(R.drawable.bird_parrot, R.raw.snd_parrot, "بَبَّغَاء", "Parrot"),
        Character(R.drawable.bird_pigeon, R.raw.snd_pigeon, "حَمَامَة", "Pigeon"),
        Character(R.drawable.bird_eagle, R.raw.snd_eagle, "نَسْر", "Eagle"),
        Character(R.drawable.bird_falcon, R.raw.snd_falcon, "صَقْر", "Falcon"),
        Character(R.drawable.bird_peacock, R.raw.snd_peacock, "طَاوُوس", "Peacock"),
        Character(R.drawable.bird_crow, R.raw.snd_crow, "غُرَاب", "Crow"),
        Character(R.drawable.bird_hoopoe, R.raw.snd_hoopoe, "هُدْهُد", "Hoopoe bird"),
        Character(R.drawable.bird_penguin, R.raw.snd_penguin, "بِطْرِيق", "Penguin"),
        Character(R.drawable.bird_flamingo, R.raw.snd_flamingo, "فْلَامِنْجُو", "Flamingo"),
    )

    val vehicles = listOf(
        Character(R.drawable.veh_car, R.raw.snd_car, "سَيَّارَة", "Car"),
        Character(R.drawable.veh_police, R.raw.snd_police, "سَيَّارَةُ شُرْطَة", "Police car"),
        Character(R.drawable.veh_ambulance, R.raw.snd_ambulance, "سَيَّارَةُ إِسْعَاف", "Ambulance"),
        Character(R.drawable.veh_firetruck, R.raw.snd_firetruck, "سَيَّارَةُ إِطْفَاء", "Fire truck"),
        Character(R.drawable.veh_airplane, R.raw.snd_airplane, "طَائِرَة", "Airplane"),
        Character(R.drawable.veh_helicopter, R.raw.snd_helicopter, "هِلِيكُوبْتَر", "Helicopter"),
        Character(R.drawable.veh_train, R.raw.snd_train, "قِطَار", "Train"),
        Character(R.drawable.veh_ship, R.raw.snd_ship, "سَفِينَة", "Ship"),
        Character(R.drawable.veh_motorcycle, R.raw.snd_motorcycle, "دَرَّاجَةٌ نَارِيَّة", "Motorcycle"),
        Character(R.drawable.veh_bicycle, R.raw.snd_bicycle, "دَرَّاجَة", "Bicycle"),
        Character(R.drawable.veh_bus, R.raw.snd_bus, "حَافِلَة", "Bus"),
    )

    val fruits = listOf(
        Character(R.drawable.fruit_apple, 0, "تُفَّاحَة", "Apple"),
        Character(R.drawable.fruit_banana, 0, "مَوْزَة", "Banana"),
        Character(R.drawable.fruit_orange, 0, "بُرْتُقَالَة", "Orange"),
        Character(R.drawable.fruit_grapes, 0, "عِنَب", "Grapes"),
        Character(R.drawable.fruit_strawberry, 0, "فَرَاوْلَة", "Strawberry"),
        Character(R.drawable.fruit_watermelon, 0, "بِطِّيخ", "Watermelon"),
        Character(R.drawable.fruit_pear, 0, "كُمَّثْرَى", "Pear"),
        Character(R.drawable.fruit_cherry, 0, "كَرَز", "Cherry"),
        Character(R.drawable.fruit_pineapple, 0, "أَنَانَاس", "Pineapple"),
        Character(R.drawable.fruit_mango, 0, "مَانْجُو", "Mango"),
        Character(R.drawable.fruit_lemon, 0, "لَيْمُون", "Lemon"),
        Character(R.drawable.fruit_peach, 0, "خَوْخ", "Peach"),
    )

    val sea = listOf(
        Character(R.drawable.sea_clownfish, 0, "سَمَكَةُ المُهَرِّج", "Clown fish"),
        Character(R.drawable.sea_bluetang, 0, "سَمَكَةٌ زَرْقَاء", "Blue fish"),
        Character(R.drawable.sea_puffer, 0, "سَمَكَةٌ مُنْتَفِخَة", "Puffer fish"),
        Character(R.drawable.sea_shark, 0, "قِرْش", "Shark"),
        Character(R.drawable.sea_whale, R.raw.snd_whale, "حُوت", "Whale"),
        Character(R.drawable.sea_dolphin, R.raw.snd_dolphin, "دُلْفِين", "Dolphin"),
        Character(R.drawable.sea_seahorse, 0, "حِصَانُ البَحْر", "Seahorse"),
        Character(R.drawable.sea_starfish, 0, "نَجْمَةُ البَحْر", "Star fish"),
        Character(R.drawable.sea_crab, 0, "سَلْطَعُون", "Crab"),
        Character(R.drawable.sea_octopus, 0, "أُخْطُبُوط", "Octopus"),
        Character(R.drawable.sea_jellyfish, 0, "قِنْدِيلُ البَحْر", "Jellyfish"),
        Character(R.drawable.sea_turtle, 0, "سُلَحْفَاة", "Turtle"),
    )

    val flags = listOf(
        Character(R.drawable.flag_sa, 0, "السُّعُودِيَّة", "Saudi"),
        Character(R.drawable.flag_eg, 0, "مِصْر", "Egypt"),
        Character(R.drawable.flag_ae, 0, "الإِمَارَات", "Emirates"),
        Character(R.drawable.flag_kw, 0, "الكُوَيْت", "Kuwait"),
        Character(R.drawable.flag_qa, 0, "قَطَر", "Qatar"),
        Character(R.drawable.flag_bh, 0, "البَحْرَيْن", "Bahrain"),
        Character(R.drawable.flag_om, 0, "عُمَان", "Oman"),
        Character(R.drawable.flag_jo, 0, "الأُرْدُن", "Jordan"),
        Character(R.drawable.flag_ps, 0, "فِلَسْطِين", "Palestine"),
        Character(R.drawable.flag_ma, 0, "المَغْرِب", "Morocco"),
        Character(R.drawable.flag_dz, 0, "الجَزَائِر", "Algeria"),
        Character(R.drawable.flag_tn, 0, "تُونِس", "Tunisia"),
        Character(R.drawable.flag_iq, 0, "العِرَاق", "Iraq"),
        Character(R.drawable.flag_lb, 0, "لُبْنَان", "Lebanon"),
        Character(R.drawable.flag_ye, 0, "اليَمَن", "Yemen"),
        Character(R.drawable.flag_sd, 0, "السُّودَان", "Sudan"),
        Character(R.drawable.flag_tr, 0, "تُرْكِيَا", "Turkey"),
        Character(R.drawable.flag_pk, 0, "بَاكِسْتَان", "Pakistan"),
        Character(R.drawable.flag_id, 0, "إِنْدُونِيسِيَا", "Indonesia"),
        Character(R.drawable.flag_my, 0, "مَالِيزِيَا", "Malaysia"),
        Character(R.drawable.flag_jp, 0, "اليَابَان", "Japan"),
        Character(R.drawable.flag_br, 0, "البَرَازِيل", "Brazil"),
    )

    fun forMode(mode: GameMode): List<Character> = when (mode) {
        GameMode.COLORS -> emptyList()
        GameMode.ANIMALS -> animals
        GameMode.VEHICLES -> vehicles
        GameMode.BIRDS -> birds
        GameMode.FRUITS -> fruits
        GameMode.SEA -> sea
        GameMode.FLAGS -> flags
    }

    val all: List<Character> get() = animals + birds + vehicles + fruits + sea + flags

    /** Spoken on each pop in colors mode, in turn. */
    val praise = listOf("سُبْحَانَ اللَّه", "الْحَمْدُ لِلَّه", "اللَّهُ أَكْبَر")
}
