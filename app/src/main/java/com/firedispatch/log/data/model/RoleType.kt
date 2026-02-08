package com.firedispatch.log.data.model

enum class RoleType(val displayName: String, val order: Int, val isMultiple: Boolean) {
    DANTAICHO("分団長", 1, false),
    FUKUDANTAICHO("副分団長", 2, false),
    SHOBOBUCHO("消防部長", 3, false),
    KEIGOBUCHO("警護部長", 4, false),
    KYUSUIHANCHO("給水班長", 5, false),
    KIKAIHANCHO("機械班長", 6, false),
    HISAKIHANCHO("火先班長", 7, false),
    KYUGOHANCHO("救護班長", 8, false),
    KEIKOUHANCHO("警交班長", 9, false),
    KYUSUIDAN("給水団員", 10, true),
    KIKAIDAN("機械団員", 11, true),
    HISAKIDAN("火先団員", 12, true),
    KYUGODAN("救護団員", 13, true),
    KEIKOUDAN("警交団員", 14, true),
    HOJODAN("補助団員", 15, true);

    companion object {
        fun fromDisplayName(name: String): RoleType? {
            return entries.find { it.displayName == name }
        }

        fun getRegularMemberRoles(): List<RoleType> {
            return listOf(KYUSUIDAN, KIKAIDAN, HISAKIDAN, KYUGODAN, KEIKOUDAN, HOJODAN)
        }

        fun getLeaderRoles(): List<RoleType> {
            return entries.filter { !it.isMultiple }
        }
    }
}
