package app.reportamelo.commons.utils

data class Country(val name: String, val regions: List<Region>)
data class Region(val name: String, val cities: List<City>)
data class City(
    val name: String,
    val legalName: String,
    val coordinates: String,
    val groupingId: String,
    val groupingName: String
)

data class FriendlyCityDistribution(
    val cityId: String,
    val firstLevel: String,
    val secondLevel: String,
    val thirdLevel: String,
    val ZipCode: String,
    val legalGroupName: String,
    val coordinates: String,
    val isCapitalCity: Int,
    val isDepartmentalCapital: Int,
    val groupingId: String,
    val groupingName: String
)

data class FriendlyCityDistributionList(val cities: List<FriendlyCityDistribution>)

fun Country.flattenToFriendlyDistribution(): FriendlyCityDistributionList {
    val flatCities = mutableListOf<FriendlyCityDistribution>()
    
    for (region in this.regions) {
        for (city in region.cities) {
            val friendlyCity = FriendlyCityDistribution(
                cityId = "",
                firstLevel = this.name,
                secondLevel = region.name,
                thirdLevel = city.name,
                ZipCode = "",
                legalGroupName = city.legalName,
                coordinates = city.coordinates,
                isCapitalCity = 0,
                isDepartmentalCapital = 1,
                groupingId = city.groupingId,
                groupingName = city.groupingName
            )
            flatCities.add(friendlyCity)
        }
    }
    
    return FriendlyCityDistributionList(flatCities)
}
