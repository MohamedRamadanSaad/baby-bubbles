package com.mramadan.babybubbles.game

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.mramadan.babybubbles.R

class AnimalInfo(
    @DrawableRes val drawable: Int,
    @RawRes val sound: Int,
    val arabic: String,
    val english: String,
)

/** Characters drawn on the design canvas; sounds are temporary synthesized placeholders. */
val AnimalCatalog = listOf(
    AnimalInfo(R.drawable.animal_cat, R.raw.animal_cat, "قطة", "Cat"),
    AnimalInfo(R.drawable.animal_dog, R.raw.animal_dog, "كلب", "Dog"),
    AnimalInfo(R.drawable.animal_cow, R.raw.animal_cow, "بقرة", "Cow"),
    AnimalInfo(R.drawable.animal_sheep, R.raw.animal_sheep, "خروف", "Sheep"),
    AnimalInfo(R.drawable.animal_duck, R.raw.animal_duck, "بطة", "Duck"),
    AnimalInfo(R.drawable.animal_chicken, R.raw.animal_chicken, "دجاجة", "Chicken"),
    AnimalInfo(R.drawable.animal_horse, R.raw.animal_horse, "حصان", "Horse"),
    AnimalInfo(R.drawable.animal_lion, R.raw.animal_lion, "أسد", "Lion"),
    AnimalInfo(R.drawable.animal_elephant, R.raw.animal_elephant, "فيل", "Elephant"),
    AnimalInfo(R.drawable.animal_frog, R.raw.animal_frog, "ضفدع", "Frog"),
    AnimalInfo(R.drawable.animal_camel, R.raw.animal_camel, "جمل", "Camel"),
    AnimalInfo(R.drawable.animal_owl, R.raw.animal_owl, "بومة", "Owl"),
)

const val DUCK_INDEX = 4
