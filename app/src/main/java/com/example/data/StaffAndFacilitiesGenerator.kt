package com.example.data

import com.example.models.*

object StaffAndFacilitiesGenerator {

    private val coachFirstNames = listOf("Steve", "Erik", "Gregg", "Nick", "Taylor", "Tyronn", "Will", "Doc", "Joe", "Jason", "Ime", "Mark", "Chris", "Rick", "Quin")
    private val coachLastNames = listOf("Kerr", "Spoelstra", "Popovich", "Nurse", "Jenkins", "Lue", "Hardy", "Rivers", "Mazzulla", "Kidd", "Udoka", "Daigneault", "Finch", "Carlisle", "Snyder")

    fun generateInitialStaff(teamName: String): TeamStaff {
        val head = HeadCoachStaff(
            id = (1000..9999).random(),
            name = "${coachFirstNames.random()} ${coachLastNames.random()}",
            level = (68..88).random(),
            salary = (3_000_000..8_500_000).random(),
            contractYears = (2..4).random(),
            offensiveSkill = (70..92).random(),
            defensiveSkill = (68..90).random(),
            motivationalSkill = (70..95).random(),
            experience = (3..18).random(),
            reputation = (65..92).random(),
            preferredStyle = PlayStyle.values().random(),
            playerDevelopment = (65..90).random(),
            gameManagement = (70..92).random()
        )

        val assistant = AssistantCoachStaff(
            id = (1000..9999).random(),
            name = "${coachFirstNames.random()} ${coachLastNames.random()}",
            level = (65..82).random(),
            salary = (800_000..1_800_000).random(),
            contractYears = 2,
            specialty = listOf("Desenvolvimento Ofensivo", "Estratégia Defensiva", "Desenvolvimento de Jovens").random()
        )

        val strength = StrengthCoach(
            id = (1000..9999).random(),
            name = "${coachFirstNames.random()} ${coachLastNames.random()}",
            level = (70..85).random(),
            salary = (600_000..1_200_000).random(),
            contractYears = 2,
            specialty = listOf("Prevenção & Força", "Velocidade & Explosão", "Resistência Cardio").random()
        )

        val scout = ScoutStaff(
            id = (1000..9999).random(),
            name = "${coachFirstNames.random()} ${coachLastNames.random()}",
            level = (70..88).random(),
            salary = (500_000..1_100_000).random(),
            contractYears = 2,
            specialty = listOf("Draft Universitário", "Mercado Internacional", "Observação da G-League").random()
        )

        val doctor = TeamDoctor(
            id = (1000..9999).random(),
            name = "Dr. ${coachFirstNames.random()} ${coachLastNames.random()}",
            level = (72..90).random(),
            salary = (700_000..1_500_000).random(),
            contractYears = 3,
            specialty = "Reabilitação Rápida & Prevenção"
        )

        val gm = ExecutiveStaff(
            id = (1000..9999).random(),
            name = "${coachFirstNames.random()} ${coachLastNames.random()}",
            level = (72..90).random(),
            salary = (2_000_000..4_500_000).random(),
            contractYears = 3,
            specialty = "Negociações Teto Salarial & Trocas",
            roleTitle = "General Manager (GM)"
        )

        return TeamStaff(
            headCoach = head,
            assistants = mutableListOf(assistant),
            strengthCoach = strength,
            scout = scout,
            teamDoctor = doctor,
            executives = mutableListOf(gm)
        )
    }

    fun generateAvailableStaffMarket(): List<StaffMember> {
        val list = mutableListOf<StaffMember>()
        val usedIds = mutableSetOf<Int>()
        fun nextUniqueMarketId(): Int {
            var id: Int
            do {
                id = (10000..99999).random()
            } while (!usedIds.add(id))
            return id
        }

        repeat(3) {
            list.add(
                HeadCoachStaff(
                    id = nextUniqueMarketId(),
                    name = "${coachFirstNames.random()} ${coachLastNames.random()}",
                    level = (65..92).random(),
                    salary = (2_500_000..9_000_000).random(),
                    contractYears = (1..4).random(),
                    offensiveSkill = (65..95).random(),
                    defensiveSkill = (65..95).random(),
                    motivationalSkill = (60..95).random(),
                    experience = (1..20).random(),
                    reputation = (50..95).random(),
                    preferredStyle = PlayStyle.values().random(),
                    playerDevelopment = (60..95).random(),
                    gameManagement = (60..95).random()
                )
            )
        }
        repeat(2) {
            list.add(
                AssistantCoachStaff(
                    id = nextUniqueMarketId(),
                    name = "${coachFirstNames.random()} ${coachLastNames.random()}",
                    level = (60..85).random(),
                    salary = (500_000..1_500_000).random(),
                    contractYears = 2,
                    specialty = listOf("Ataque Posicionado", "Defesa Perimetral", "Fundamentos de Arremesso", "Treino de Rookies").random()
                )
            )
        }
        repeat(2) {
            list.add(
                StrengthCoach(
                    id = nextUniqueMarketId(),
                    name = "${coachFirstNames.random()} ${coachLastNames.random()}",
                    level = (65..88).random(),
                    salary = (400_000..1_100_000).random(),
                    contractYears = 2,
                    specialty = listOf("Flexibilidade & Biomecânica", "Massa Magra e Potência", "Condicionamento Físico").random()
                )
            )
        }
        repeat(2) {
            list.add(
                ScoutStaff(
                    id = nextUniqueMarketId(),
                    name = "${coachFirstNames.random()} ${coachLastNames.random()}",
                    level = (65..90).random(),
                    salary = (400_000..1_000_000).random(),
                    contractYears = 2,
                    specialty = listOf("NCAA Division 1", "Talentos da Europa", "Prospecção Colegial").random()
                )
            )
        }
        repeat(2) {
            list.add(
                TeamDoctor(
                    id = nextUniqueMarketId(),
                    name = "Dr. ${coachFirstNames.random()} ${coachLastNames.random()}",
                    level = (70..92).random(),
                    salary = (600_000..1_600_000).random(),
                    contractYears = 3,
                    specialty = listOf("Ortopedia Esportiva", "Fisioterapia Avançada", "Recuperação Muscular").random()
                )
            )
        }
        return list
    }

    fun generateInitialSponsorships(): MutableList<SponsorshipDeal> {
        return mutableListOf(
            SponsorshipDeal(
                brandName = "Nike High-Flyer",
                type = "Global",
                annualAmount = 12_000_000,
                yearsRemaining = 3,
                goalDescription = "Chegar aos Playoffs",
                goalBonus = 2_500_000
            ),
            SponsorshipDeal(
                brandName = "Gatorade Hydro Pro",
                type = "Nacional",
                annualAmount = 6_500_000,
                yearsRemaining = 2,
                goalDescription = "Vencer 40+ jogos",
                goalBonus = 1_000_000
            ),
            SponsorshipDeal(
                brandName = "Chase Center Arena Bank",
                type = "Regional",
                annualAmount = 4_000_000,
                yearsRemaining = 1,
                goalDescription = "Manter público acima de 85%",
                goalBonus = 500_000
            )
        )
    }
}
