TownyWarsMCC
============

Main code
------------

TownyWars is the main class and handles startup and shutdown of the plugin. There's a lot less code in it than there used to be.

TownyWarsDatabase is a class that handles all database operations, including opening and closing the database, and loading and saving of towns, nations, residents, wars, and deaths. A lot of it is a bunch of string operations for inserting and recalling data from the database tables. Some of it could be made a little clearer by using multiple tables with foreign keys, but it's probably not a priority since it works as is.

TownyWarsListener is a listener class that checks for town and nation-related events (except for creating a new nation, which is handled separately because that event is broken in Towny). So it handles things like residents being added and removed from towns and towns joining and leaving nations.

TownyWarsPlayerListener is a listener class that checks for player-related events (and also creating nations). So it handles things like players joining and leaving, taking damage, and dying.

War is the class that holds all the information about a war (which takes place between two objects that implement the Attackable interface; see below). It keeps track of the type and status of the war, the number of deaths on each side, and whether peace has been offered. It also has some fields which aren't fully implemented yet (custom peace requests and requested money condition for peace). There's a lot of stuff in this class, and I've had a lot of trouble deciding what should go in it and what should go in WarManager (see next item).

WarManager is a class that started out life as the controller for wars (starting and ending them, and modifying their properties). A good portion of its function I decided belonged in War, so it doesn't do as much as it used to. I'm not sure if it's strictly necessary to have, actually, because some of its methods mainly just call methods in War. I think that it could just get absorbed into War, even. . .

WarExecutor is a class that manages the player's chat interactions with TownyWars. It contains all the functions needed to process player input and decide what to do with it (like showing the status of a war, informing players about ongoing wars, creating and editing wars). This class needs considerable work, especially with regard to creating and editing wars, and informing players properly about wars (like when they log in, for instance).


Objects/Interfaces
---------------------

TownyWarsObject is a generic class that only contains the uuid and name fields, along with get and set methods for them. This is used as the base for all the other objects, mainly to save re-coding of those fields and methods. Thus, every object in TownyWars contains at least a uuid and a name.

Attackable is an interface that specifies the names of a bunch of methods. Any class that implements it will return an object that can be in a war (whether a town, a nation, or a Coalition, or something else entirely). I did this to make it easier to deal with towns and nations in wars; now the War class doesn't have to care whether a nation is attacking another nation, or just a town.

TownyWarsOrg is a class that extends TownyWarsObject and implements the methods described by the Attackable interface (I use the name "org" to refer to any organization that players or other organizations belong to). It serves as the basis for TownyWarsTown and TownyWarsNation. Mainly, it saves re-coding of a bunch of functions and lets the War treat both towns and nations essentially equivalently by implementing Attackable.

TownyWarsTown is the TownyWars version of Towny's Town object. It is a kind of wrapper object that stores a reference to the Towny Town object that it is associated with, and provides a way to look up the TownyWarsTown object that a Towny Town object is associated with. It also contains special functions for handling DP calculations (which actually override the ones specified in TownyWarsOrg).

TownyWarsNation is the TownyWars version of Towny's Nation object. It does the same thing as above, except for Towny Nation objects.
