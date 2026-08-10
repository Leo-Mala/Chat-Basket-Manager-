package com.example.data

import com.example.models.*

object NbaDataGenerator {

    private var idCounter = 1

    private fun nextId(): Int = idCounter++

    private val arenas = listOf(
        Arena("State Farm Arena", "Atlanta", 17044, 1999),
        Arena("TD Garden", "Boston", 18624, 1995),
        Arena("Barclays Center", "Brooklyn", 17732, 2012),
        Arena("Spectrum Center", "Charlotte", 19077, 2005),
        Arena("United Center", "Chicago", 20917, 1994),
        Arena("Rocket Mortgage FieldHouse", "Cleveland", 19432, 1994),
        Arena("Little Caesars Arena", "Detroit", 20332, 2017),
        Arena("Gainbridge Fieldhouse", "Indianapolis", 17923, 1999),
        Arena("Kaseya Center", "Miami", 19600, 2000),
        Arena("Fiserv Forum", "Milwaukee", 17341, 2018),
        Arena("Madison Square Garden", "New York", 19812, 1968),
        Arena("Kia Center", "Orlando", 18846, 2010),
        Arena("Wells Fargo Center", "Philadelphia", 19500, 1996),
        Arena("Scotiabank Arena", "Toronto", 19800, 1999),
        Arena("Capital One Arena", "Washington", 20333, 1997),
        Arena("American Airlines Center", "Dallas", 19200, 2001),
        Arena("Ball Arena", "Denver", 19520, 1999),
        Arena("Chase Center", "San Francisco", 18064, 2019),
        Arena("Toyota Center", "Houston", 18055, 2003),
        Arena("Intuit Dome", "Inglewood", 18000, 2024),
        Arena("Crypto.com Arena", "Los Angeles", 18997, 1999),
        Arena("FedExForum", "Memphis", 17794, 2004),
        Arena("Target Center", "Minneapolis", 18024, 1990),
        Arena("Smoothie King Center", "New Orleans", 16867, 2002),
        Arena("Paycom Center", "Oklahoma City", 18203, 2008),
        Arena("Footprint Center", "Phoenix", 17071, 1992),
        Arena("Moda Center", "Portland", 19393, 1995),
        Arena("Golden 1 Center", "Sacramento", 17608, 2016),
        Arena("Frost Bank Center", "San Antonio", 18581, 2002),
        Arena("Delta Center", "Salt Lake City", 18306, 1991)
    )

    private val rosters = mapOf(
        // 1. Atlanta Hawks
        "Atlanta Hawks" to listOf(
            Player(nextId(), "Trae Young", "PG", 93, 91, 45, 35, 95, 85, 26),
            Player(nextId(), "Jalen Johnson", "SF", 86, 82, 80, 78, 75, 88, 24),
            Player(nextId(), "Dyson Daniels", "SG", 84, 78, 88, 70, 75, 85, 22),
            Player(nextId(), "Onyeka Okongwu", "C", 83, 75, 82, 85, 68, 80, 25),
            Player(nextId(), "Zaccharie Risacher", "SF", 79, 78, 75, 70, 65, 80, 20),
            Player(nextId(), "De'Andre Hunter", "SF", 83, 80, 78, 72, 68, 78, 27),
            Player(nextId(), "Bogdan Bogdanović", "SG", 79, 82, 68, 55, 74, 70, 32),
            Player(nextId(), "Larry Nance Jr.", "PF", 77, 72, 76, 74, 64, 74, 32),
            Player(nextId(), "Garrison Mathews", "SG", 77, 78, 70, 52, 68, 72, 28),
            Player(nextId(), "Nickeil Alexander-Walker", "SG", 80, 78, 80, 65, 72, 82, 27),
            Player(nextId(), "Mouhamed Gueye", "PF", 75, 70, 76, 80, 65, 78, 23),
            Player(nextId(), "Vít Krejčí", "PG", 76, 74, 72, 60, 78, 72, 25),
            Player(nextId(), "Keaton Wallace", "PG", 72, 72, 70, 55, 78, 76, 26),
            Player(nextId(), "Luke Kennard", "SG", 79, 85, 60, 55, 70, 70, 29),
            Player(nextId(), "Asa Newell", "PF", 72, 68, 74, 78, 60, 75, 20),
            Player(nextId(), "CJ McCollum", "SG", 86, 84, 72, 55, 80, 76, 34),
            Player(nextId(), "Corey Kispert", "SF", 78, 80, 70, 60, 68, 76, 26),
            Player(nextId(), "Gabe Vincent", "PG", 76, 74, 72, 55, 78, 76, 29)
        ),

        // 2. Boston Celtics
        "Boston Celtics" to listOf(
            Player(nextId(), "Jayson Tatum", "SF", 95, 94, 88, 82, 85, 92, 27),
            Player(nextId(), "Jaylen Brown", "SG", 91, 90, 85, 78, 80, 90, 29),
            Player(nextId(), "Derrick White", "PG", 87, 86, 84, 70, 88, 82, 31),
            Player(nextId(), "Jrue Holiday", "PG", 84, 82, 84, 65, 86, 78, 34),
            Player(nextId(), "Payton Pritchard", "PG", 82, 84, 70, 60, 82, 76, 28),
            Player(nextId(), "Al Horford", "C", 79, 76, 78, 80, 68, 68, 38),
            Player(nextId(), "Luke Kornet", "C", 78, 72, 74, 80, 62, 70, 29),
            Player(nextId(), "Sam Hauser", "PF", 76, 78, 68, 62, 70, 74, 28),
            Player(nextId(), "Neemias Queta", "C", 77, 70, 75, 82, 60, 78, 26),
            Player(nextId(), "Jordan Walsh", "SF", 73, 72, 74, 68, 66, 76, 21),
            Player(nextId(), "Baylor Scheierman", "SG", 74, 76, 72, 60, 70, 74, 25),
            Player(nextId(), "Xavier Tillman Sr.", "C", 76, 72, 78, 80, 68, 70, 27),
            Player(nextId(), "Dalano Banton", "PG", 75, 74, 72, 60, 78, 78, 25),
            Player(nextId(), "Chris Boucher", "PF", 74, 70, 74, 72, 62, 72, 32),
            Player(nextId(), "Anton Watson", "PF", 72, 70, 72, 70, 62, 74, 24),
            Player(nextId(), "Jaden Springer", "PG", 74, 72, 74, 55, 72, 78, 22),
            Player(nextId(), "Lamar Stevens", "SF", 73, 70, 74, 62, 64, 76, 27),
            Player(nextId(), "Mike Conley", "PG", 80, 78, 76, 55, 86, 74, 37)
        ),

        // 3. Brooklyn Nets
        "Brooklyn Nets" to listOf(
            Player(nextId(), "Nic Claxton", "C", 84, 70, 82, 90, 65, 82, 25),
            Player(nextId(), "Cameron Johnson", "SF", 84, 82, 76, 68, 74, 78, 28),
            Player(nextId(), "Noah Clowney", "PF", 75, 72, 76, 78, 62, 76, 20),
            Player(nextId(), "Egor Dёmin", "PG", 76, 72, 70, 60, 80, 74, 21),
            Player(nextId(), "Ochai Agbaji", "SG", 77, 76, 76, 55, 70, 80, 24),
            Player(nextId(), "Day'Ron Sharpe", "C", 77, 68, 74, 84, 60, 74, 23),
            Player(nextId(), "Ziaire Williams", "SF", 77, 74, 76, 62, 66, 80, 23),
            Player(nextId(), "Keon Johnson", "SG", 75, 74, 72, 60, 70, 80, 22),
            Player(nextId(), "Jalen Wilson", "PF", 73, 70, 72, 74, 60, 72, 24),
            Player(nextId(), "Tyson Etienne", "PG", 71, 72, 70, 55, 76, 74, 26),
            Player(nextId(), "Trendon Watford", "PF", 74, 70, 72, 72, 62, 74, 25),
            Player(nextId(), "Dariq Whitehead", "SF", 72, 72, 70, 60, 64, 74, 20),
            Player(nextId(), "Cam Thomas", "SG", 86, 88, 70, 55, 70, 80, 23),
            Player(nextId(), "Michael Porter Jr.", "PF", 83, 85, 74, 72, 70, 80, 26),
            Player(nextId(), "Mikel Brown Jr.", "PG", 73, 72, 70, 55, 76, 76, 21),
            Player(nextId(), "Nolan Traoré", "SG", 72, 72, 70, 55, 70, 76, 20),
            Player(nextId(), "Ben Saraf", "SF", 71, 70, 70, 62, 64, 74, 20),
            Player(nextId(), "Danny Wolf", "C", 70, 65, 70, 76, 60, 70, 21)
        ),

        // 4. Charlotte Hornets
        "Charlotte Hornets" to listOf(
            Player(nextId(), "LaMelo Ball", "PG", 87, 86, 72, 60, 92, 82, 23),
            Player(nextId(), "Miles Bridges", "PF", 83, 82, 76, 80, 72, 85, 27),
            Player(nextId(), "Kon Knueppel", "SF", 82, 82, 78, 72, 74, 80, 20),
            Player(nextId(), "Grant Williams", "PF", 78, 76, 78, 74, 68, 74, 26),
            Player(nextId(), "Jusuf Nurkić", "C", 77, 72, 74, 80, 64, 70, 30),
            Player(nextId(), "Mark Williams", "C", 80, 70, 78, 86, 60, 78, 23),
            Player(nextId(), "Brandon Miller", "SF", 85, 84, 78, 70, 78, 86, 22),
            Player(nextId(), "Josh Green", "SG", 76, 74, 76, 60, 72, 80, 24),
            Player(nextId(), "Tre Mann", "PG", 77, 76, 70, 55, 80, 80, 23),
            Player(nextId(), "Moussa Diabaté", "C", 76, 68, 74, 80, 60, 74, 24),
            Player(nextId(), "Nick Smith Jr.", "SG", 76, 76, 70, 55, 72, 78, 21),
            Player(nextId(), "K.J. Simpson", "PG", 72, 72, 70, 55, 76, 76, 22),
            Player(nextId(), "Seth Curry", "SG", 79, 84, 60, 50, 68, 70, 34),
            Player(nextId(), "Cody Martin", "SF", 76, 74, 76, 68, 72, 78, 29),
            Player(nextId(), "Vasilije Micic", "PG", 75, 74, 68, 52, 82, 70, 30),
            Player(nextId(), "Sion James", "SG", 71, 70, 72, 55, 68, 74, 22),
            Player(nextId(), "Ryan Kalkbrenner", "C", 70, 65, 72, 76, 58, 68, 23),
            Player(nextId(), "Tidjane Salaün", "PF", 72, 68, 72, 70, 62, 76, 19)
        ),

        // 5. Chicago Bulls
        "Chicago Bulls" to listOf(
            Player(nextId(), "Josh Giddey", "PG", 84, 78, 74, 70, 88, 78, 22),
            Player(nextId(), "Coby White", "SG", 83, 84, 72, 55, 78, 80, 25),
            Player(nextId(), "Nikola Vucevic", "C", 85, 82, 70, 86, 72, 70, 33),
            Player(nextId(), "Patrick Williams", "PF", 77, 76, 78, 74, 68, 76, 23),
            Player(nextId(), "Ayo Dosunmu", "SG", 79, 78, 76, 60, 80, 80, 25),
            Player(nextId(), "Matas Buzelis", "PF", 76, 74, 74, 76, 65, 78, 21),
            Player(nextId(), "Zach Collins", "C", 75, 72, 74, 80, 64, 72, 27),
            Player(nextId(), "Jevon Carter", "PG", 74, 74, 72, 55, 78, 76, 28),
            Player(nextId(), "Rob Dillingham", "PG", 74, 76, 70, 52, 78, 78, 22),
            Player(nextId(), "Noa Essengue", "PF", 72, 70, 74, 72, 62, 76, 22),
            Player(nextId(), "Kevin Huerter", "SG", 78, 80, 72, 60, 74, 76, 26),
            Player(nextId(), "Jalen Smith", "C", 75, 70, 72, 78, 60, 74, 25),
            Player(nextId(), "Collin Sexton", "PG", 82, 80, 72, 55, 80, 84, 27),
            Player(nextId(), "Trentyn Flowers", "SG", 73, 72, 72, 55, 70, 76, 21),
            Player(nextId(), "Rocco Zikarsky", "C", 70, 65, 72, 76, 58, 68, 19),
            Player(nextId(), "Dalen Terry", "SF", 73, 70, 74, 62, 66, 76, 22),
            Player(nextId(), "Julian Phillips", "SF", 72, 70, 72, 62, 64, 76, 21),
            Player(nextId(), "Adama Sanogo", "PF", 71, 68, 72, 70, 60, 72, 23)
        ),

        // 6. Cleveland Cavaliers
        "Cleveland Cavaliers" to listOf(
            Player(nextId(), "Donovan Mitchell", "SG", 94, 92, 80, 60, 82, 88, 28),
            Player(nextId(), "Evan Mobley", "PF", 92, 78, 84, 88, 72, 80, 23),
            Player(nextId(), "Darius Garland", "PG", 89, 86, 72, 50, 90, 82, 25),
            Player(nextId(), "Jarrett Allen", "C", 88, 74, 80, 90, 64, 78, 26),
            Player(nextId(), "De'Andre Hunter", "SF", 82, 80, 78, 72, 68, 78, 27),
            Player(nextId(), "Caris LeVert", "SG", 79, 78, 74, 58, 76, 80, 30),
            Player(nextId(), "Ty Jerome", "PG", 81, 78, 76, 55, 82, 76, 27),
            Player(nextId(), "Max Strus", "SF", 79, 80, 72, 60, 70, 76, 28),
            Player(nextId(), "Isaac Okoro", "SF", 78, 76, 80, 68, 72, 80, 24),
            Player(nextId(), "Dean Wade", "PF", 76, 74, 76, 72, 64, 74, 27),
            Player(nextId(), "Sam Merrill", "SG", 74, 76, 68, 52, 68, 72, 28),
            Player(nextId(), "Jaylon Tyson", "SG", 74, 74, 72, 55, 70, 74, 22),
            Player(nextId(), "Georges Niang", "PF", 78, 78, 70, 68, 66, 72, 31),
            Player(nextId(), "Thomas Bryant", "C", 75, 72, 72, 78, 60, 72, 27),
            Player(nextId(), "Tristan Enaruna", "SF", 72, 70, 72, 62, 64, 74, 25),
            Player(nextId(), "Nae'Qwan Tomlin", "PF", 72, 70, 72, 70, 62, 74, 24),
            Player(nextId(), "JT Thor", "PF", 73, 70, 74, 70, 62, 74, 22),
            Player(nextId(), "Luke Travers", "SG", 71, 70, 72, 55, 68, 74, 23)
        ),

        // 7. Detroit Pistons
        "Detroit Pistons" to listOf(
            Player(nextId(), "Cade Cunningham", "PG", 92, 84, 78, 65, 88, 80, 23),
            Player(nextId(), "Jalen Duren", "C", 83, 72, 78, 88, 62, 82, 21),
            Player(nextId(), "Jaden Ivey", "SG", 82, 80, 74, 55, 78, 86, 22),
            Player(nextId(), "Ausar Thompson", "SF", 80, 74, 82, 72, 70, 88, 21),
            Player(nextId(), "Tobias Harris", "PF", 79, 78, 76, 74, 70, 74, 32),
            Player(nextId(), "Tim Hardaway Jr.", "SG", 77, 78, 68, 50, 70, 76, 32),
            Player(nextId(), "Marcus Sasser", "PG", 75, 76, 70, 52, 78, 76, 24),
            Player(nextId(), "Isaiah Stewart", "C", 78, 72, 78, 84, 60, 76, 23),
            Player(nextId(), "Simone Fontecchio", "SF", 75, 76, 72, 60, 66, 76, 28),
            Player(nextId(), "Ron Holland", "SF", 74, 72, 74, 62, 66, 80, 20),
            Player(nextId(), "Chaz Lanier", "SG", 72, 74, 70, 55, 68, 76, 23),
            Player(nextId(), "Charles Bediako", "C", 71, 65, 72, 76, 58, 72, 22),
            Player(nextId(), "Evan Fournier", "SG", 74, 76, 68, 50, 68, 72, 32),
            Player(nextId(), "Stanley Umude", "PF", 72, 70, 72, 70, 62, 74, 25),
            Player(nextId(), "Danilo Gallinari", "PF", 75, 74, 70, 68, 62, 72, 36),
            Player(nextId(), "Malik Beasley", "SG", 79, 82, 70, 52, 68, 74, 27),
            Player(nextId(), "Tosan Evbuomwan", "SF", 73, 70, 72, 62, 64, 74, 23),
            Player(nextId(), "Ebuka Okorie", "PF", 71, 68, 72, 70, 60, 74, 22)
        ),

        // 8. Indiana Pacers
        "Indiana Pacers" to listOf(
            Player(nextId(), "Tyrese Haliburton", "PG", 90, 88, 72, 55, 94, 80, 25),
            Player(nextId(), "Pascal Siakam", "PF", 87, 86, 80, 78, 74, 82, 30),
            Player(nextId(), "Myles Turner", "C", 85, 80, 80, 86, 62, 78, 28),
            Player(nextId(), "Buddy Hield", "SG", 83, 86, 70, 52, 68, 74, 32),
            Player(nextId(), "Bennedict Mathurin", "SG", 81, 80, 72, 58, 72, 82, 22),
            Player(nextId(), "Aaron Nesmith", "SF", 79, 78, 78, 68, 68, 80, 25),
            Player(nextId(), "T.J. McConnell", "PG", 78, 76, 74, 55, 82, 76, 32),
            Player(nextId(), "Andrew Nembhard", "PG", 78, 76, 76, 55, 82, 76, 25),
            Player(nextId(), "Isaiah Jackson", "C", 76, 70, 76, 80, 60, 78, 24),
            Player(nextId(), "Jalen Smith", "PF", 75, 72, 74, 72, 62, 74, 25),
            Player(nextId(), "Jordan Nwora", "SF", 74, 74, 72, 65, 66, 76, 26),
            Player(nextId(), "Kendall Brown", "SG", 72, 72, 70, 55, 68, 76, 22),
            Player(nextId(), "Asa Newell", "PF", 72, 68, 74, 78, 60, 75, 20),
            Player(nextId(), "Taelon Peter", "SG", 71, 70, 70, 52, 68, 74, 22),
            Player(nextId(), "Johnny Furphy", "SF", 73, 72, 72, 62, 64, 76, 21),
            Player(nextId(), "Kobe Brown", "PF", 74, 72, 74, 70, 62, 74, 24),
            Player(nextId(), "Larry Nance Jr.", "PF", 77, 72, 76, 74, 64, 74, 32),
            Player(nextId(), "Tony Bradley", "C", 73, 68, 72, 76, 60, 70, 28)
        ),

        // 9. Miami Heat
        "Miami Heat" to listOf(
            Player(nextId(), "Jimmy Butler", "SF", 89, 86, 88, 70, 80, 86, 35),
            Player(nextId(), "Bam Adebayo", "C", 88, 82, 86, 88, 74, 82, 27),
            Player(nextId(), "Tyler Herro", "SG", 84, 86, 70, 55, 76, 78, 25),
            Player(nextId(), "Terry Rozier", "PG", 82, 80, 72, 55, 82, 80, 30),
            Player(nextId(), "Jaime Jaquez Jr.", "SF", 80, 78, 78, 65, 72, 80, 23),
            Player(nextId(), "Kevin Love", "PF", 78, 78, 70, 74, 68, 68, 36),
            Player(nextId(), "Duncan Robinson", "SG", 79, 84, 68, 50, 66, 70, 30),
            Player(nextId(), "Caleb Martin", "SF", 78, 76, 78, 62, 68, 78, 29),
            Player(nextId(), "Haywood Highsmith", "PF", 76, 74, 76, 70, 64, 76, 28),
            Player(nextId(), "Orlando Robinson", "C", 74, 70, 72, 76, 60, 72, 24),
            Player(nextId(), "Nikola Jovic", "PF", 75, 72, 74, 72, 64, 74, 21),
            Player(nextId(), "Jamal Cain", "SF", 73, 70, 72, 62, 64, 76, 25),
            Player(nextId(), "Cole Swider", "PF", 72, 72, 68, 70, 60, 72, 25),
            Player(nextId(), "Pelle Larsson", "SG", 72, 72, 70, 55, 70, 74, 23),
            Player(nextId(), "Kel'el Ware", "C", 74, 68, 74, 78, 60, 76, 21),
            Player(nextId(), "Patty Mills", "PG", 75, 76, 70, 50, 78, 72, 37),
            Player(nextId(), "Thomas Bryant", "C", 75, 72, 72, 78, 60, 72, 27)
        ),

        // 10. Milwaukee Bucks
        "Milwaukee Bucks" to listOf(
            Player(nextId(), "Giannis Antetokounmpo", "PF", 98, 86, 90, 88, 82, 96, 30),
            Player(nextId(), "Damian Lillard", "PG", 91, 92, 72, 55, 88, 82, 34),
            Player(nextId(), "Khris Middleton", "SF", 86, 86, 78, 62, 76, 76, 33),
            Player(nextId(), "Brook Lopez", "C", 84, 80, 82, 86, 62, 70, 36),
            Player(nextId(), "Bobby Portis", "PF", 81, 78, 76, 80, 66, 76, 29),
            Player(nextId(), "Cole Anthony", "PG", 79, 78, 72, 55, 78, 78, 24),
            Player(nextId(), "Pat Connaughton", "SG", 77, 76, 74, 60, 70, 78, 31),
            Player(nextId(), "Jae Crowder", "SF", 76, 74, 76, 65, 68, 72, 34),
            Player(nextId(), "MarJon Beauchamp", "SF", 75, 74, 74, 62, 66, 78, 24),
            Player(nextId(), "Amir Coffey", "SG", 74, 74, 72, 60, 68, 76, 27),
            Player(nextId(), "Alex Antetokounmpo", "SF", 72, 70, 72, 62, 64, 76, 22),
            Player(nextId(), "Thanasis Antetokounmpo", "SF", 73, 70, 74, 62, 64, 76, 30),
            Player(nextId(), "A.J. Green", "SG", 73, 74, 70, 55, 66, 74, 25),
            Player(nextId(), "Chris Livingston", "SF", 72, 70, 72, 62, 64, 76, 21),
            Player(nextId(), "Robin Lopez", "C", 72, 68, 72, 76, 60, 70, 36),
            Player(nextId(), "Lindell Wigginton", "PG", 72, 72, 70, 52, 76, 74, 26),
            Player(nextId(), "Jevon Carter", "PG", 74, 74, 72, 55, 78, 76, 28),
            Player(nextId(), "Tyler Smith", "PF", 72, 68, 72, 72, 60, 74, 20)
        ),

        // 11. New York Knicks
        "New York Knicks" to listOf(
            Player(nextId(), "Jalen Brunson", "PG", 93, 90, 72, 55, 92, 80, 28),
            Player(nextId(), "Karl-Anthony Towns", "C", 93, 88, 74, 86, 78, 76, 29),
            Player(nextId(), "Julius Randle", "PF", 87, 82, 76, 82, 74, 82, 29),
            Player(nextId(), "OG Anunoby", "SF", 84, 80, 86, 68, 70, 84, 27),
            Player(nextId(), "Josh Hart", "SG", 82, 76, 78, 70, 72, 80, 29),
            Player(nextId(), "Donte DiVincenzo", "SG", 80, 82, 74, 60, 74, 78, 27),
            Player(nextId(), "Mitchell Robinson", "C", 79, 68, 78, 88, 60, 76, 26),
            Player(nextId(), "Isaiah Hartenstein", "C", 78, 70, 76, 82, 64, 72, 26),
            Player(nextId(), "Miles McBride", "PG", 76, 74, 74, 52, 78, 78, 24),
            Player(nextId(), "Precious Achiuwa", "PF", 75, 70, 74, 74, 62, 76, 25),
            Player(nextId(), "Quentin Grimes", "SG", 77, 76, 76, 55, 70, 78, 24),
            Player(nextId(), "Bojan Bogdanovic", "SF", 74, 76, 68, 62, 66, 70, 35),
            Player(nextId(), "Jericho Sims", "C", 73, 65, 72, 76, 58, 76, 26),
            Player(nextId(), "Jacob Toppin", "SF", 72, 70, 72, 62, 64, 76, 24),
            Player(nextId(), "Duane Washington Jr.", "PG", 73, 74, 70, 52, 76, 74, 25),
            Player(nextId(), "Dmytro Skapintsev", "C", 71, 65, 70, 74, 58, 68, 26),
            Player(nextId(), "Charlie Brown Jr.", "SF", 71, 70, 72, 62, 62, 74, 27),
            Player(nextId(), "Jaylen Martin", "SG", 70, 70, 68, 55, 66, 74, 20)
        ),

        // 12. Orlando Magic
        "Orlando Magic" to listOf(
            Player(nextId(), "Paolo Banchero", "PF", 88, 84, 78, 76, 76, 84, 22),
            Player(nextId(), "Franz Wagner", "SF", 85, 82, 78, 62, 76, 82, 23),
            Player(nextId(), "Jalen Suggs", "PG", 82, 78, 82, 60, 80, 84, 23),
            Player(nextId(), "Wendell Carter Jr.", "C", 80, 74, 78, 82, 68, 76, 25),
            Player(nextId(), "Cole Anthony", "PG", 79, 78, 72, 55, 78, 78, 24),
            Player(nextId(), "Gary Harris", "SG", 78, 76, 76, 55, 68, 76, 30),
            Player(nextId(), "Moritz Wagner", "C", 77, 72, 74, 78, 64, 74, 27),
            Player(nextId(), "Anthony Black", "PG", 76, 74, 76, 55, 74, 78, 21),
            Player(nextId(), "Jett Howard", "SF", 74, 74, 72, 60, 66, 76, 21),
            Player(nextId(), "Caleb Houstan", "SF", 73, 72, 70, 58, 64, 74, 22),
            Player(nextId(), "Goga Bitadze", "C", 75, 68, 74, 80, 62, 72, 25),
            Player(nextId(), "Desmond Bane", "SG", 85, 86, 74, 55, 74, 78, 26),
            Player(nextId(), "Tristan da Silva", "SF", 73, 72, 72, 62, 64, 74, 23),
            Player(nextId(), "Jonathan Isaac", "PF", 76, 70, 78, 76, 62, 78, 27),
            Player(nextId(), "Trevelin Queen", "SG", 72, 72, 70, 55, 68, 76, 27),
            Player(nextId(), "Admiral Schofield", "SF", 72, 70, 72, 62, 64, 74, 27),
            Player(nextId(), "Kevon Harris", "SG", 72, 72, 70, 55, 68, 74, 27),
            Player(nextId(), "Mac McClung", "PG", 71, 72, 68, 52, 74, 78, 25)
        ),

        // 13. Philadelphia 76ers
        "Philadelphia 76ers" to listOf(
            Player(nextId(), "Joel Embiid", "C", 96, 88, 84, 90, 74, 78, 30),
            Player(nextId(), "Tyrese Maxey", "PG", 90, 88, 76, 55, 86, 90, 24),
            Player(nextId(), "Tobias Harris", "PF", 83, 80, 76, 74, 70, 78, 32),
            Player(nextId(), "Kyle Lowry", "PG", 81, 78, 78, 55, 84, 74, 38),
            Player(nextId(), "Kelly Oubre Jr.", "SF", 80, 78, 76, 65, 68, 82, 28),
            Player(nextId(), "De'Anthony Melton", "SG", 79, 76, 82, 60, 72, 80, 26),
            Player(nextId(), "Paul Reed", "C", 77, 70, 74, 80, 62, 76, 25),
            Player(nextId(), "Furkan Korkmaz", "SG", 75, 76, 68, 52, 66, 72, 27),
            Player(nextId(), "Jaden Springer", "PG", 74, 72, 74, 55, 72, 78, 22),
            Player(nextId(), "Montrezl Harrell", "PF", 76, 72, 70, 74, 60, 74, 30),
            Player(nextId(), "Danuel House Jr.", "SF", 74, 72, 74, 62, 64, 76, 30),
            Player(nextId(), "Terquavion Smith", "PG", 72, 72, 70, 52, 74, 76, 22),
            Player(nextId(), "Mo Bamba", "C", 75, 68, 74, 80, 58, 72, 26),
            Player(nextId(), "Patrick Beverley", "PG", 76, 72, 78, 55, 78, 74, 36),
            Player(nextId(), "Marcus Morris Sr.", "PF", 75, 76, 72, 70, 62, 70, 35),
            Player(nextId(), "Nicolas Batum", "SF", 74, 72, 74, 62, 68, 70, 36),
            Player(nextId(), "Jeff Dowtin", "PG", 72, 72, 70, 52, 76, 74, 27),
            Player(nextId(), "Jared Springer", "SG", 73, 72, 72, 55, 68, 76, 22)
        ),

        // 14. Toronto Raptors
        "Toronto Raptors" to listOf(
            Player(nextId(), "Scottie Barnes", "PF", 86, 78, 82, 78, 76, 86, 23),
            Player(nextId(), "RJ Barrett", "SF", 83, 80, 76, 62, 74, 82, 24),
            Player(nextId(), "Immanuel Quickley", "PG", 82, 80, 74, 55, 84, 80, 25),
            Player(nextId(), "Jakob Poeltl", "C", 81, 72, 78, 86, 68, 74, 29),
            Player(nextId(), "Dennis Schroder", "PG", 80, 78, 74, 55, 80, 78, 31),
            Player(nextId(), "Gary Trent Jr.", "SG", 79, 80, 72, 55, 68, 76, 25),
            Player(nextId(), "Ochai Agbaji", "SG", 77, 76, 76, 55, 70, 80, 24),
            Player(nextId(), "Jalen McDaniels", "PF", 75, 72, 76, 68, 64, 78, 26),
            Player(nextId(), "Christian Koloko", "C", 74, 68, 74, 76, 60, 72, 24),
            Player(nextId(), "Thaddeus Young", "PF", 76, 72, 76, 70, 66, 72, 36),
            Player(nextId(), "Gradey Dick", "SG", 74, 74, 72, 55, 68, 76, 21),
            Player(nextId(), "Javon Freeman-Liberty", "PG", 73, 72, 70, 55, 76, 78, 24),
            Player(nextId(), "Jordan Nwora", "SF", 74, 74, 72, 65, 66, 76, 26),
            Player(nextId(), "D.J. Carton", "PG", 72, 72, 70, 52, 76, 74, 24),
            Player(nextId(), "Jalen Harris", "SG", 72, 72, 70, 55, 68, 74, 25),
            Player(nextId(), "Kira Lewis Jr.", "PG", 74, 72, 70, 55, 78, 80, 23),
            Player(nextId(), "Andrew Nembhard", "PG", 78, 76, 76, 55, 82, 76, 25),
            Player(nextId(), "Chris Boucher", "PF", 74, 70, 74, 72, 62, 72, 32)
        ),

        // 15. Washington Wizards
        "Washington Wizards" to listOf(
            Player(nextId(), "Jordan Poole", "SG", 84, 84, 72, 55, 78, 82, 25),
            Player(nextId(), "Kyle Kuzma", "PF", 83, 80, 76, 74, 70, 80, 29),
            Player(nextId(), "Deni Avdija", "SF", 80, 78, 78, 68, 74, 78, 23),
            Player(nextId(), "Daniel Gafford", "C", 81, 72, 76, 84, 60, 76, 26),
            Player(nextId(), "Tyus Jones", "PG", 79, 76, 74, 52, 84, 76, 28),
            Player(nextId(), "Corey Kispert", "SG", 78, 80, 70, 52, 68, 76, 25),
            Player(nextId(), "Marvin Bagley III", "C", 77, 74, 72, 78, 60, 74, 25),
            Player(nextId(), "Bilal Coulibaly", "SF", 76, 74, 76, 62, 66, 80, 20),
            Player(nextId(), "Johnny Davis", "SG", 74, 74, 72, 55, 68, 74, 23),
            Player(nextId(), "Jared Butler", "PG", 73, 72, 70, 52, 76, 74, 24),
            Player(nextId(), "Anthony Gill", "PF", 72, 70, 72, 70, 62, 72, 32),
            Player(nextId(), "Mike Muscala", "C", 73, 70, 70, 74, 62, 68, 33),
            Player(nextId(), "Patrick Baldwin Jr.", "SF", 72, 72, 70, 62, 64, 74, 22),
            Player(nextId(), "Ryan Rollins", "PG", 73, 72, 70, 55, 76, 78, 22),
            Player(nextId(), "Eugene Omoruyi", "SF", 72, 70, 72, 62, 64, 74, 27),
            Player(nextId(), "Delon Wright", "PG", 75, 72, 76, 55, 78, 74, 32),
            Player(nextId(), "Landry Shamet", "SG", 74, 76, 70, 52, 68, 72, 27),
            Player(nextId(), "Richaun Holmes", "C", 74, 68, 72, 78, 60, 72, 31)
        ),

        // 16. Dallas Mavericks
        "Dallas Mavericks" to listOf(
            Player(nextId(), "Luka Doncic", "PG", 96, 94, 78, 74, 92, 82, 25),
            Player(nextId(), "Kyrie Irving", "PG", 92, 92, 74, 55, 86, 84, 32),
            Player(nextId(), "Anthony Davis", "PF", 95, 84, 88, 92, 70, 82, 31),
            Player(nextId(), "Cooper Flagg", "SF", 82, 78, 82, 72, 74, 84, 19),
            Player(nextId(), "Daniel Gafford", "C", 83, 72, 76, 84, 60, 76, 26),
            Player(nextId(), "Klay Thompson", "SG", 79, 82, 72, 55, 68, 70, 35),
            Player(nextId(), "Max Christie", "SG", 79, 76, 76, 58, 70, 78, 21),
            Player(nextId(), "Brandon Williams", "PG", 76, 74, 72, 55, 78, 78, 25),
            Player(nextId(), "Ryan Nembhard", "PG", 73, 72, 70, 52, 80, 74, 22),
            Player(nextId(), "Dereck Lively II", "C", 79, 70, 74, 84, 60, 78, 21),
            Player(nextId(), "Josh Green", "SG", 78, 76, 76, 60, 72, 80, 24),
            Player(nextId(), "Jaden Hardy", "SG", 77, 76, 70, 52, 72, 78, 22),
            Player(nextId(), "Maxi Kleber", "PF", 76, 74, 76, 70, 64, 72, 32),
            Player(nextId(), "Dante Exum", "PG", 77, 74, 74, 58, 78, 80, 29),
            Player(nextId(), "Tim Hardaway Jr.", "SG", 80, 82, 68, 50, 70, 76, 32),
            Player(nextId(), "Marvin Bagley III", "PF", 77, 74, 72, 78, 60, 74, 25),
            Player(nextId(), "Moussa Cisse", "C", 72, 65, 72, 76, 58, 72, 22),
            Player(nextId(), "Olivier-Maxence Prosper", "PF", 75, 72, 74, 72, 62, 76, 22)
        ),

        // 17. Denver Nuggets
        "Denver Nuggets" to listOf(
            Player(nextId(), "Nikola Jokic", "C", 98, 94, 72, 88, 95, 74, 29),
            Player(nextId(), "Jamal Murray", "PG", 86, 86, 76, 55, 82, 80, 27),
            Player(nextId(), "Michael Porter Jr.", "SF", 85, 86, 72, 70, 68, 78, 26),
            Player(nextId(), "Christian Braun", "SG", 83, 80, 78, 65, 72, 84, 24),
            Player(nextId(), "Aaron Gordon", "PF", 82, 76, 82, 78, 72, 86, 29),
            Player(nextId(), "Russell Westbrook", "PG", 81, 76, 74, 60, 82, 88, 36),
            Player(nextId(), "Kentavious Caldwell-Pope", "SG", 81, 80, 82, 55, 70, 78, 31),
            Player(nextId(), "DaRon Holmes II", "PF", 78, 74, 76, 78, 66, 78, 22),
            Player(nextId(), "Julian Strawther", "SF", 77, 76, 74, 62, 68, 78, 22),
            Player(nextId(), "Reggie Jackson", "PG", 77, 76, 70, 52, 78, 76, 34),
            Player(nextId(), "Zeke Nnaji", "PF", 75, 72, 74, 74, 62, 76, 23),
            Player(nextId(), "Peyton Watson", "SF", 76, 74, 78, 68, 66, 80, 22),
            Player(nextId(), "DeAndre Jordan", "C", 74, 68, 72, 80, 58, 70, 36),
            Player(nextId(), "Bruce Brown", "SG", 78, 76, 78, 60, 72, 80, 28),
            Player(nextId(), "Trevon Brazile", "PF", 73, 70, 74, 72, 62, 76, 22),
            Player(nextId(), "Tarris Reed Jr.", "C", 72, 65, 72, 76, 58, 70, 21),
            Player(nextId(), "Vlatko Cancar", "PF", 73, 70, 72, 70, 62, 72, 27),
            Player(nextId(), "Bryce Hopkins", "SF", 72, 70, 72, 62, 64, 74, 22)
        ),

        // 18. Golden State Warriors
        "Golden State Warriors" to listOf(
            Player(nextId(), "Stephen Curry", "PG", 95, 98, 70, 50, 88, 78, 36),
            Player(nextId(), "Jimmy Butler", "SF", 85, 84, 86, 70, 78, 82, 36),
            Player(nextId(), "Draymond Green", "PF", 85, 74, 86, 78, 84, 74, 34),
            Player(nextId(), "Andrew Wiggins", "SF", 84, 82, 80, 68, 72, 86, 29),
            Player(nextId(), "Chris Paul", "PG", 83, 82, 74, 55, 90, 70, 39),
            Player(nextId(), "Jonathan Kuminga", "PF", 82, 76, 78, 72, 68, 88, 22),
            Player(nextId(), "Moses Moody", "SG", 79, 78, 74, 58, 70, 78, 22),
            Player(nextId(), "Brandin Podziemski", "SG", 78, 76, 74, 62, 76, 76, 21),
            Player(nextId(), "Kevon Looney", "C", 78, 68, 76, 82, 60, 74, 28),
            Player(nextId(), "Gary Payton II", "PG", 77, 74, 82, 60, 70, 86, 32),
            Player(nextId(), "De'Anthony Melton", "SG", 76, 76, 80, 55, 72, 78, 26),
            Player(nextId(), "Trayce Jackson-Davis", "C", 75, 70, 72, 78, 62, 76, 24),
            Player(nextId(), "Pat Spencer", "PG", 72, 72, 70, 55, 76, 74, 26),
            Player(nextId(), "Gui Santos", "SF", 73, 72, 72, 62, 64, 76, 22),
            Player(nextId(), "Lester Quinones", "SG", 74, 74, 70, 55, 68, 76, 23),
            Player(nextId(), "L.J. Cryer", "SG", 72, 74, 68, 52, 70, 74, 24),
            Player(nextId(), "Quinten Post", "C", 70, 65, 70, 76, 58, 68, 25),
            Player(nextId(), "Charles Bassey", "C", 73, 65, 72, 78, 58, 72, 24)
        ),

        // 19. Houston Rockets
        "Houston Rockets" to listOf(
            Player(nextId(), "Kevin Durant", "SF", 93, 94, 86, 70, 80, 82, 37),
            Player(nextId(), "Alperen Sengun", "C", 87, 84, 74, 86, 78, 76, 22),
            Player(nextId(), "Jalen Green", "SG", 85, 84, 72, 52, 76, 88, 22),
            Player(nextId(), "Fred VanVleet", "PG", 84, 82, 76, 55, 86, 76, 30),
            Player(nextId(), "Dillon Brooks", "SF", 82, 78, 84, 65, 68, 80, 28),
            Player(nextId(), "Jabari Smith Jr.", "PF", 81, 78, 80, 78, 66, 78, 21),
            Player(nextId(), "Clint Capela", "C", 81, 68, 76, 88, 60, 72, 31),
            Player(nextId(), "Amen Thompson", "SF", 79, 74, 80, 70, 72, 88, 21),
            Player(nextId(), "Cam Whitmore", "SF", 78, 76, 74, 62, 68, 84, 20),
            Player(nextId(), "Tari Eason", "PF", 80, 76, 82, 76, 66, 82, 23),
            Player(nextId(), "Aaron Holiday", "PG", 76, 76, 72, 52, 76, 78, 28),
            Player(nextId(), "Jock Landale", "C", 75, 70, 72, 76, 60, 72, 29),
            Player(nextId(), "Reggie Bullock Jr.", "SF", 74, 74, 72, 60, 64, 72, 33),
            Player(nextId(), "Jeff Green", "PF", 74, 72, 72, 68, 62, 70, 38),
            Player(nextId(), "Steven Adams", "C", 76, 68, 76, 88, 60, 72, 31),
            Player(nextId(), "Jae'Sean Tate", "SF", 75, 72, 76, 65, 66, 78, 29),
            Player(nextId(), "Boban Marjanovic", "C", 73, 68, 70, 76, 58, 68, 36),
            Player(nextId(), "Nate Hinton", "SG", 72, 70, 72, 55, 68, 76, 25)
        ),

        // 20. LA Clippers
        "LA Clippers" to listOf(
            Player(nextId(), "Kawhi Leonard", "SF", 94, 92, 90, 70, 78, 86, 33),
            Player(nextId(), "Paul George", "SF", 91, 90, 86, 68, 76, 84, 34),
            Player(nextId(), "James Harden", "PG", 88, 86, 74, 65, 90, 76, 35),
            Player(nextId(), "Bradley Beal", "SG", 88, 86, 74, 55, 80, 82, 31),
            Player(nextId(), "Russell Westbrook", "PG", 82, 76, 74, 60, 82, 88, 36),
            Player(nextId(), "Derrick Jones Jr.", "SF", 81, 78, 82, 70, 68, 88, 27),
            Player(nextId(), "Kris Dunn", "PG", 79, 74, 82, 62, 80, 78, 30),
            Player(nextId(), "Ivica Zubac", "C", 80, 72, 76, 84, 60, 74, 27),
            Player(nextId(), "Norman Powell", "SG", 82, 84, 72, 52, 68, 78, 31),
            Player(nextId(), "Terance Mann", "SG", 79, 78, 78, 62, 74, 80, 28),
            Player(nextId(), "Bones Hyland", "PG", 77, 76, 70, 52, 78, 76, 24),
            Player(nextId(), "Mason Plumlee", "C", 76, 70, 74, 78, 62, 72, 34),
            Player(nextId(), "Amir Coffey", "SF", 75, 74, 74, 60, 66, 76, 27),
            Player(nextId(), "Nicolas Batum", "PF", 74, 72, 74, 68, 64, 70, 36),
            Player(nextId(), "Kobe Brown", "PF", 74, 72, 74, 70, 62, 74, 24),
            Player(nextId(), "Jordan Miller", "SG", 73, 72, 70, 55, 68, 76, 24),
            Player(nextId(), "Yanic Konan Niederhauser", "PF", 72, 68, 72, 72, 60, 74, 22),
            Player(nextId(), "Keaton Wagler", "PG", 71, 70, 70, 52, 74, 74, 22)
        ),

        // 21. Los Angeles Lakers
        "Los Angeles Lakers" to listOf(
            Player(nextId(), "LeBron James", "SF", 96, 92, 86, 78, 90, 88, 40),
            Player(nextId(), "Anthony Davis", "C", 94, 84, 88, 92, 70, 82, 31),
            Player(nextId(), "Austin Reaves", "SG", 85, 84, 76, 58, 80, 78, 26),
            Player(nextId(), "Rui Hachimura", "PF", 81, 80, 74, 72, 68, 78, 26),
            Player(nextId(), "Gabe Vincent", "PG", 78, 76, 76, 55, 78, 76, 28),
            Player(nextId(), "Jarred Vanderbilt", "PF", 77, 70, 82, 78, 66, 80, 25),
            Player(nextId(), "Christian Wood", "C", 79, 76, 72, 78, 62, 74, 29),
            Player(nextId(), "Max Christie", "SG", 74, 74, 72, 55, 68, 76, 21),
            Player(nextId(), "Jalen Hood-Schifino", "PG", 72, 70, 70, 52, 74, 74, 21),
            Player(nextId(), "Cam Reddish", "SF", 75, 72, 76, 62, 64, 78, 25),
            Player(nextId(), "Maxwell Lewis", "SF", 73, 72, 72, 62, 64, 76, 22),
            Player(nextId(), "D'Angelo Russell", "PG", 83, 82, 72, 55, 84, 76, 28),
            Player(nextId(), "Taurean Prince", "SF", 76, 74, 76, 62, 66, 74, 31),
            Player(nextId(), "Jaxson Hayes", "C", 74, 68, 72, 78, 60, 76, 24),
            Player(nextId(), "Colin Castleton", "C", 72, 65, 70, 76, 58, 70, 24),
            Player(nextId(), "Skylar Mays", "PG", 73, 72, 70, 52, 76, 74, 27),
            Player(nextId(), "Maxwell Christie", "SG", 74, 74, 72, 55, 68, 76, 21),
            Player(nextId(), "Quincy Olivari", "PG", 71, 70, 70, 52, 74, 74, 23)
        ),

        // 22. Memphis Grizzlies
        "Memphis Grizzlies" to listOf(
            Player(nextId(), "Ja Morant", "PG", 92, 88, 74, 60, 86, 92, 25),
            Player(nextId(), "Jaren Jackson Jr.", "PF", 91, 82, 86, 78, 62, 80, 25),
            Player(nextId(), "Desmond Bane", "SG", 85, 86, 74, 55, 74, 78, 26),
            Player(nextId(), "Marcus Smart", "PG", 83, 78, 84, 60, 78, 80, 30),
            Player(nextId(), "Steven Adams", "C", 80, 68, 76, 88, 60, 72, 31),
            Player(nextId(), "Santi Aldama", "PF", 77, 76, 72, 70, 64, 74, 23),
            Player(nextId(), "Luke Kennard", "SG", 79, 84, 68, 55, 70, 70, 28),
            Player(nextId(), "Xavier Tillman", "C", 76, 70, 76, 78, 62, 72, 25),
            Player(nextId(), "David Roddy", "SF", 75, 74, 74, 65, 66, 76, 23),
            Player(nextId(), "John Konchar", "SG", 74, 72, 76, 65, 68, 74, 28),
            Player(nextId(), "Kenneth Lofton Jr.", "PF", 74, 72, 70, 72, 62, 74, 22),
            Player(nextId(), "Vince Williams Jr.", "SF", 73, 72, 74, 62, 64, 76, 24),
            Player(nextId(), "Scotty Pippen Jr.", "PG", 74, 72, 72, 55, 78, 78, 24),
            Player(nextId(), "GG Jackson II", "PF", 75, 72, 74, 72, 62, 76, 20),
            Player(nextId(), "Jake LaRavia", "SF", 73, 72, 72, 62, 66, 74, 23),
            Player(nextId(), "Ziaire Williams", "SF", 77, 74, 76, 62, 66, 80, 23),
            Player(nextId(), "DeJon Jarreau", "SG", 72, 70, 72, 55, 68, 76, 27),
            Player(nextId(), "Trey Jemison", "C", 71, 65, 72, 76, 58, 70, 25)
        ),

        // 23. Minnesota Timberwolves
        "Minnesota Timberwolves" to listOf(
            Player(nextId(), "Anthony Edwards", "SG", 94, 90, 82, 55, 78, 94, 23),
            Player(nextId(), "Karl-Anthony Towns", "C", 90, 88, 74, 86, 78, 76, 28),
            Player(nextId(), "Rudy Gobert", "C", 85, 68, 84, 92, 60, 74, 32),
            Player(nextId(), "Jaden McDaniels", "SF", 81, 78, 82, 65, 68, 82, 24),
            Player(nextId(), "Mike Conley", "PG", 80, 78, 76, 55, 86, 74, 37),
            Player(nextId(), "Naz Reid", "PF", 79, 78, 72, 74, 64, 76, 25),
            Player(nextId(), "Kyle Anderson", "SF", 78, 74, 78, 68, 72, 74, 31),
            Player(nextId(), "Nickeil Alexander-Walker", "SG", 77, 76, 76, 55, 70, 78, 26),
            Player(nextId(), "Jordan McLaughlin", "PG", 74, 74, 72, 52, 78, 76, 28),
            Player(nextId(), "Luka Garza", "C", 73, 72, 68, 74, 60, 70, 25),
            Player(nextId(), "Troy Brown Jr.", "SF", 74, 72, 74, 62, 66, 76, 25),
            Player(nextId(), "Wendell Moore Jr.", "SG", 73, 72, 70, 55, 68, 76, 23),
            Player(nextId(), "Josh Minott", "SF", 73, 70, 74, 62, 64, 78, 22),
            Player(nextId(), "Leonard Miller", "PF", 73, 70, 72, 72, 62, 76, 21),
            Player(nextId(), "Jaylen Clark", "SG", 72, 70, 72, 55, 68, 76, 23),
            Player(nextId(), "Matt Ryan", "SF", 73, 74, 68, 62, 64, 72, 27),
            Player(nextId(), "Daishen Nix", "PG", 72, 72, 70, 55, 76, 74, 23),
            Player(nextId(), "Justin Jackson", "SF", 72, 70, 72, 62, 64, 74, 29)
        ),

        // 24. New Orleans Pelicans
        "New Orleans Pelicans" to listOf(
            Player(nextId(), "Zion Williamson", "PF", 91, 86, 74, 82, 72, 94, 24),
            Player(nextId(), "Brandon Ingram", "SF", 88, 86, 76, 65, 78, 80, 27),
            Player(nextId(), "CJ McCollum", "SG", 85, 84, 72, 55, 80, 76, 33),
            Player(nextId(), "Jonas Valanciunas", "C", 82, 76, 74, 86, 68, 70, 32),
            Player(nextId(), "Herb Jones", "SF", 80, 76, 84, 62, 68, 82, 26),
            Player(nextId(), "Trey Murphy III", "SG", 79, 80, 72, 55, 66, 78, 24),
            Player(nextId(), "Jose Alvarado", "PG", 77, 74, 76, 52, 80, 78, 26),
            Player(nextId(), "Larry Nance Jr.", "PF", 76, 72, 76, 74, 64, 74, 31),
            Player(nextId(), "Dyson Daniels", "SG", 78, 74, 80, 60, 72, 82, 22),
            Player(nextId(), "Naji Marshall", "SF", 75, 74, 74, 62, 66, 76, 25),
            Player(nextId(), "Kira Lewis Jr.", "PG", 74, 72, 70, 55, 78, 80, 23),
            Player(nextId(), "Willy Hernangomez", "C", 73, 68, 70, 76, 60, 70, 30),
            Player(nextId(), "Jordan Hawkins", "SG", 74, 76, 68, 55, 68, 76, 22),
            Player(nextId(), "E.J. Liddell", "PF", 73, 70, 74, 72, 62, 74, 24),
            Player(nextId(), "Dereon Seabron", "SG", 72, 70, 72, 55, 68, 76, 24),
            Player(nextId(), "Jalen Crutcher", "PG", 72, 72, 70, 52, 76, 74, 25),
            Player(nextId(), "Matkovic", "PF", 71, 68, 72, 70, 60, 74, 23),
            Player(nextId(), "Trey Jemison", "C", 72, 65, 72, 76, 58, 72, 25)
        ),

        // 25. Oklahoma City Thunder
        "Oklahoma City Thunder" to listOf(
            Player(nextId(), "Shai Gilgeous-Alexander", "PG", 98, 92, 80, 60, 90, 88, 26),
            Player(nextId(), "Chet Holmgren", "C", 88, 82, 82, 84, 70, 80, 22),
            Player(nextId(), "Jalen Williams", "SF", 86, 82, 80, 65, 76, 84, 23),
            Player(nextId(), "Josh Giddey", "PG", 82, 76, 74, 68, 86, 78, 22),
            Player(nextId(), "Luguentz Dort", "SG", 80, 76, 84, 55, 68, 82, 25),
            Player(nextId(), "Cason Wallace", "PG", 79, 76, 78, 55, 74, 78, 21),
            Player(nextId(), "Aaron Wiggins", "SG", 79, 76, 76, 60, 72, 80, 26),
            Player(nextId(), "Jaylin Williams", "PF", 76, 74, 72, 70, 64, 74, 22),
            Player(nextId(), "Tre Mann", "PG", 77, 76, 72, 52, 78, 80, 23),
            Player(nextId(), "Ousmane Dieng", "SF", 74, 72, 72, 60, 64, 76, 21),
            Player(nextId(), "Isaiah Joe", "SG", 75, 76, 72, 55, 68, 76, 25),
            Player(nextId(), "Kenrich Williams", "SF", 74, 72, 74, 62, 66, 74, 30),
            Player(nextId(), "Jalen Williams", "PF", 76, 74, 72, 70, 64, 74, 22),
            Player(nextId(), "Aleksej Pokusevski", "PF", 74, 70, 74, 72, 66, 76, 22),
            Player(nextId(), "Lindy Waters III", "SG", 73, 72, 72, 55, 68, 74, 27),
            Player(nextId(), "Adam Flagler", "SG", 72, 72, 70, 55, 68, 74, 25),
            Player(nextId(), "Olivier Sarr", "C", 72, 65, 72, 76, 58, 70, 25),
            Player(nextId(), "Keyontae Johnson", "SF", 72, 70, 72, 62, 64, 74, 24)
        ),

        // 26. Phoenix Suns
        "Phoenix Suns" to listOf(
            Player(nextId(), "Kevin Durant", "SF", 93, 94, 86, 70, 80, 84, 36),
            Player(nextId(), "Devin Booker", "SG", 90, 92, 78, 55, 84, 84, 28),
            Player(nextId(), "Bradley Beal", "SG", 88, 86, 74, 55, 80, 80, 31),
            Player(nextId(), "Deandre Ayton", "C", 84, 78, 76, 86, 64, 78, 26),
            Player(nextId(), "Jusuf Nurkic", "C", 81, 72, 74, 84, 68, 70, 30),
            Player(nextId(), "Grayson Allen", "SG", 80, 82, 72, 55, 70, 76, 29),
            Player(nextId(), "Eric Gordon", "SG", 79, 80, 70, 52, 68, 74, 36),
            Player(nextId(), "Nassir Little", "SF", 76, 74, 76, 62, 66, 78, 24),
            Player(nextId(), "Drew Eubanks", "C", 75, 70, 72, 76, 60, 72, 27),
            Player(nextId(), "Josh Okogie", "SF", 77, 72, 78, 65, 64, 80, 26),
            Player(nextId(), "Jordan Goodwin", "PG", 74, 72, 74, 55, 76, 78, 25),
            Player(nextId(), "Yuta Watanabe", "SF", 73, 72, 72, 60, 64, 72, 29),
            Player(nextId(), "Damion Lee", "SG", 73, 74, 70, 52, 68, 72, 32),
            Player(nextId(), "Ish Wainright", "PF", 73, 70, 74, 70, 62, 74, 30),
            Player(nextId(), "Saben Lee", "PG", 72, 72, 70, 52, 76, 78, 25),
            Player(nextId(), "Udoka Azubuike", "C", 72, 65, 72, 78, 58, 72, 25),
            Player(nextId(), "Bol Bol", "C", 74, 70, 72, 76, 60, 74, 25),
            Player(nextId(), "Keita Bates-Diop", "SF", 74, 72, 74, 62, 64, 74, 28)
        ),

        // 27. Portland Trail Blazers
        "Portland Trail Blazers" to listOf(
            Player(nextId(), "Anfernee Simons", "SG", 84, 84, 70, 52, 78, 82, 25),
            Player(nextId(), "Scoot Henderson", "PG", 82, 76, 74, 55, 86, 88, 21),
            Player(nextId(), "Jerami Grant", "PF", 83, 80, 78, 68, 72, 80, 30),
            Player(nextId(), "Deandre Ayton", "C", 84, 78, 76, 86, 64, 78, 26),
            Player(nextId(), "Shaedon Sharpe", "SG", 80, 78, 72, 55, 70, 86, 21),
            Player(nextId(), "Malcolm Brogdon", "PG", 81, 80, 74, 55, 82, 76, 32),
            Player(nextId(), "Robert Williams III", "C", 78, 68, 78, 84, 60, 76, 27),
            Player(nextId(), "Jabari Walker", "PF", 75, 72, 74, 72, 62, 76, 22),
            Player(nextId(), "Kris Murray", "SF", 74, 72, 74, 60, 64, 76, 23),
            Player(nextId(), "Duop Reath", "C", 73, 70, 72, 74, 60, 70, 27),
            Player(nextId(), "Toumani Camara", "PF", 76, 72, 74, 70, 62, 78, 24),
            Player(nextId(), "Sidy Cissoko", "SF", 73, 70, 72, 62, 64, 76, 22),
            Player(nextId(), "Rayan Rupert", "SG", 72, 72, 70, 55, 68, 76, 20),
            Player(nextId(), "Justin Minaya", "SF", 72, 70, 72, 62, 64, 74, 25),
            Player(nextId(), "Skal Labissiere", "C", 73, 68, 72, 76, 60, 70, 28),
            Player(nextId(), "Devonte' Graham", "PG", 74, 74, 70, 52, 78, 74, 29),
            Player(nextId(), "Dalano Banton", "PG", 75, 74, 72, 60, 78, 78, 25),
            Player(nextId(), "Moses Brown", "C", 73, 65, 72, 78, 58, 72, 25)
        ),

        // 28. Sacramento Kings
        "Sacramento Kings" to listOf(
            Player(nextId(), "De'Aaron Fox", "PG", 88, 86, 76, 55, 86, 90, 27),
            Player(nextId(), "Domantas Sabonis", "C", 89, 84, 74, 88, 80, 76, 28),
            Player(nextId(), "DeMar DeRozan", "SF", 87, 86, 76, 62, 74, 80, 35),
            Player(nextId(), "Malik Monk", "SG", 82, 80, 72, 55, 78, 82, 26),
            Player(nextId(), "Keegan Murray", "PF", 81, 80, 78, 72, 68, 78, 24),
            Player(nextId(), "Kevin Huerter", "SG", 79, 80, 72, 55, 70, 76, 26),
            Player(nextId(), "Alex Len", "C", 75, 68, 74, 78, 62, 70, 31),
            Player(nextId(), "Trey Lyles", "PF", 76, 74, 72, 70, 64, 74, 28),
            Player(nextId(), "Davion Mitchell", "PG", 77, 74, 76, 55, 78, 78, 26),
            Player(nextId(), "Chris Duarte", "SG", 74, 74, 72, 52, 68, 76, 27),
            Player(nextId(), "Sasha Vezenkov", "PF", 74, 72, 72, 70, 62, 72, 29),
            Player(nextId(), "Keon Ellis", "SG", 73, 72, 72, 55, 68, 76, 24),
            Player(nextId(), "Kessler Edwards", "SF", 73, 70, 74, 62, 64, 76, 24),
            Player(nextId(), "Jalen Slawson", "PF", 72, 68, 72, 70, 62, 74, 25),
            Player(nextId(), "Jordan Ford", "PG", 72, 72, 70, 52, 76, 74, 26),
            Player(nextId(), "Mason Jones", "SG", 72, 72, 70, 55, 68, 74, 26),
            Player(nextId(), "Neemias Queta", "C", 74, 68, 74, 80, 60, 74, 25),
            Player(nextId(), "Fardaws Aimaq", "C", 71, 65, 70, 76, 58, 68, 24)
        ),

        // 29. San Antonio Spurs
        "San Antonio Spurs" to listOf(
            Player(nextId(), "Victor Wembanyama", "C", 95, 86, 90, 90, 78, 88, 21),
            Player(nextId(), "Devin Vassell", "SG", 84, 82, 78, 55, 74, 80, 24),
            Player(nextId(), "Keldon Johnson", "SF", 82, 80, 76, 68, 72, 82, 25),
            Player(nextId(), "Jeremy Sochan", "PF", 79, 72, 80, 74, 68, 80, 21),
            Player(nextId(), "Tre Jones", "PG", 78, 76, 74, 52, 82, 78, 25),
            Player(nextId(), "Zach Collins", "C", 77, 74, 72, 76, 64, 72, 27),
            Player(nextId(), "Malaki Branham", "SG", 76, 76, 70, 52, 72, 76, 21),
            Player(nextId(), "Blake Wesley", "PG", 74, 72, 72, 52, 76, 78, 21),
            Player(nextId(), "Sandro Mamukelashvili", "PF", 73, 70, 72, 70, 62, 72, 25),
            Player(nextId(), "Julian Champagnie", "SF", 75, 74, 74, 62, 66, 76, 23),
            Player(nextId(), "Dominick Barlow", "PF", 74, 72, 74, 70, 62, 74, 21),
            Player(nextId(), "Charles Bassey", "C", 73, 65, 72, 78, 58, 72, 24),
            Player(nextId(), "Chris Boucher", "PF", 74, 70, 74, 72, 62, 72, 32),
            Player(nextId(), "Bismack Biyombo", "C", 72, 65, 72, 78, 58, 68, 33),
            Player(nextId(), "Cedi Osman", "SF", 74, 74, 72, 62, 66, 74, 29),
            Player(nextId(), "Mamadi Diakite", "PF", 72, 68, 72, 70, 60, 74, 27),
            Player(nextId(), "RaiQuan Gray", "PF", 72, 68, 72, 70, 62, 74, 25),
            Player(nextId(), "David Duke Jr.", "PG", 72, 70, 72, 55, 74, 78, 25)
        ),

        // 30. Utah Jazz
        "Utah Jazz" to listOf(
            Player(nextId(), "Lauri Markkanen", "PF", 87, 84, 78, 76, 68, 80, 27),
            Player(nextId(), "John Collins", "PF", 83, 80, 74, 80, 66, 78, 27),
            Player(nextId(), "Collin Sexton", "PG", 82, 80, 72, 55, 80, 84, 25),
            Player(nextId(), "Keyonte George", "SG", 79, 78, 72, 52, 78, 78, 21),
            Player(nextId(), "Walker Kessler", "C", 80, 68, 76, 86, 60, 74, 23),
            Player(nextId(), "Jordan Clarkson", "SG", 81, 80, 70, 55, 76, 78, 32),
            Player(nextId(), "Ochai Agbaji", "SG", 77, 76, 76, 55, 70, 80, 24),
            Player(nextId(), "Kelly Olynyk", "C", 78, 74, 72, 78, 68, 72, 33),
            Player(nextId(), "Talen Horton-Tucker", "PG", 76, 74, 74, 52, 78, 78, 24),
            Player(nextId(), "Brice Sensabaugh", "SF", 74, 74, 70, 62, 64, 74, 21),
            Player(nextId(), "Kyle Anderson", "SF", 78, 74, 78, 68, 72, 74, 31),
            Player(nextId(), "Mo Bamba", "C", 75, 68, 74, 80, 58, 72, 26),
            Player(nextId(), "Kris Dunn", "PG", 76, 72, 78, 55, 78, 78, 30),
            Player(nextId(), "Luka Samanic", "PF", 74, 72, 72, 72, 62, 74, 25),
            Player(nextId(), "Micah Potter", "C", 73, 68, 70, 76, 60, 70, 26),
            Player(nextId(), "Johnny Juzang", "SG", 73, 74, 70, 55, 68, 74, 25),
            Player(nextId(), "Darius Bazley", "SF", 74, 70, 74, 68, 64, 76, 24),
            Player(nextId(), "Jason Preston", "PG", 72, 70, 72, 55, 76, 74, 25)
        )
    )

    private fun Player.deepCopy(): Player = copy(
        overall = overall, shooting = shooting, defense = defense, rebound = rebound,
        passing = passing, athleticism = athleticism, age = age, xp = 0, trainings = 0,
        injured = false, injuryDays = 0, careerPoints = 0, careerRebounds = 0,
        careerAssists = 0, careerSteals = 0, careerBlocks = 0, careerGames = 0,
        championships = 0, mvps = 0, seasonPoints = 0, seasonRebounds = 0,
        seasonAssists = 0, seasonSteals = 0, seasonBlocks = 0, seasonGames = 0
    )

    fun getAllTeams(): List<NbaTeam> {
        val teamNames = listOf(
            // Leste (índices 0 a 14)
            "Atlanta Hawks", "Boston Celtics", "Brooklyn Nets", "Charlotte Hornets", "Chicago Bulls",
            "Cleveland Cavaliers", "Detroit Pistons", "Indiana Pacers", "Miami Heat", "Milwaukee Bucks",
            "New York Knicks", "Orlando Magic", "Philadelphia 76ers", "Toronto Raptors", "Washington Wizards",
            // Oeste (índices 15 a 29)
            "Dallas Mavericks", "Denver Nuggets", "Golden State Warriors", "Houston Rockets", "LA Clippers",
            "Los Angeles Lakers", "Memphis Grizzlies", "Minnesota Timberwolves", "New Orleans Pelicans",
            "Oklahoma City Thunder", "Phoenix Suns", "Portland Trail Blazers", "Sacramento Kings",
            "San Antonio Spurs", "Utah Jazz"
        )
        return teamNames.mapIndexed { index, name ->
            val arena = arenas[index]
            // Never expose the mutable template instances stored in `rosters`.
            // A new career must receive a deep copy so simulation/evolution cannot
            // mutate the static seed data used by a later career in the same process.
            val players = (rosters[name] ?: generateRandomPlayers(name)).map { it.deepCopy() }.onEach {
                it.overall = it.calculateOverall()
            }
            NbaTeam(
                name = name,
                city = arena.city,
                abbreviation = getAbbreviation(name),
                conference = if (index < 15) "East" else "West",
                arena = arena,
                players = players
            )
        }
    }

    private fun getAbbreviation(name: String): String = when (name) {
        "Atlanta Hawks" -> "ATL"
        "Boston Celtics" -> "BOS"
        "Brooklyn Nets" -> "BKN"
        "Charlotte Hornets" -> "CHA"
        "Chicago Bulls" -> "CHI"
        "Cleveland Cavaliers" -> "CLE"
        "Dallas Mavericks" -> "DAL"
        "Denver Nuggets" -> "DEN"
        "Detroit Pistons" -> "DET"
        "Golden State Warriors" -> "GSW"
        "Houston Rockets" -> "HOU"
        "Indiana Pacers" -> "IND"
        "LA Clippers" -> "LAC"
        "Los Angeles Lakers" -> "LAL"
        "Memphis Grizzlies" -> "MEM"
        "Milwaukee Bucks" -> "MIL"
        "Minnesota Timberwolves" -> "MIN"
        "New Orleans Pelicans" -> "NOP"
        "New York Knicks" -> "NYK"
        "Oklahoma City Thunder" -> "OKC"
        "Orlando Magic" -> "ORL"
        "Philadelphia 76ers" -> "PHI"
        "Phoenix Suns" -> "PHX"
        "Portland Trail Blazers" -> "POR"
        "Sacramento Kings" -> "SAC"
        "San Antonio Spurs" -> "SAS"
        "Toronto Raptors" -> "TOR"
        "Utah Jazz" -> "UTA"
        "Washington Wizards" -> "WAS"
        else -> name.take(3).uppercase()
    }

    private fun generateRandomPlayers(teamName: String): List<Player> {
        val positions = listOf("PG", "SG", "SF", "PF", "C")
        return positions.map {
            val overall = (70..85).random()
            Player(
                id = nextId(),
                name = "${teamName} Player ${it}",
                position = it,
                overall = overall,
                shooting = overall - (5..15).random(),
                defense = overall - (5..15).random(),
                rebound = overall - (5..15).random(),
                passing = overall - (5..15).random(),
                athleticism = overall - (5..15).random()
            )
        }
    }
}
