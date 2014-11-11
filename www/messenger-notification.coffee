# Static class for various utils
class Utils

    ###*
    Remove all items from array that are satisfying the supplied condition function.
    @param {Function} condition Callback called with one param - item. Called for each item to evaluate
        wich items to remove. Should return true on item we want to remove.
        If undefined or null, then remove all items.
    @param {Function} afterRemove Callback called on each removed item. Called after the item was removed.
        Three params: removed item, its original index, and new array (not containing the item)
    @param {Function} beforeRemove Callback called on each removed item. Called before the item remove.
        Three params: removed item, its index, and array (before removal)
    @return {number} Number of removed items
    ###
    @removeAll: (array, condition, afterRemove, beforeRemove) ->
        Utils.checkArgs('aFF', 'removeAll()', arguments)
        # todo remove all can be done more effectively
        if not condition?
            condition = -> true

        toRemove = []
        for item in array
            if condition(item) then toRemove.push(item)

        for item in toRemove
            i = array.indexOf(item)
            beforeRemove?(item, i, array)
            array.splice(i, 1)
            afterRemove?(item, i, array)
        return toRemove.length


    ###*
    @param {string} types String containing letters for desired types
        's' - string
        'n' - number
        'f' - function
        'b' - boolean
        'o' - object
        'u' - undefined
        '0' - null (zero symbol)

        'a' - array
        '?' - defined
        '.' - dont care

        Capital letter variants - can be also undefined/null
    ###
    @checkArgs: (types, funcName, args) ->

        for type, index in types
            arg = args[index]
            ret = switch type.toLowerCase()
                when 's' then typeof arg == typeof ''
                when 'n' then typeof arg == typeof 0
                when 'f' then typeof arg == typeof (->)
                when 'b' then typeof arg == typeof true
                when 'o' then typeof arg == typeof {} and not Utils.typeIsArray(arg)
                when 'u' then typeof arg == typeof undefined
                when '0' then typeof arg == typeof null

                when 'a' then Utils.typeIsArray(arg)
                when '?' then arg?
                when '.' then true

                else throw new Error("Type '#{type}' not supported")

            # if we have upper case variant and arg is not defined, then its also ok
            if type != '?' and type != '0' and type == type.toUpperCase() and not arg?
                ret = true

            if not ret then throw new Error("#{funcName} arg ##{index} is of the wrong type ('#{typeof arg}' instead of '#{type}')")

        return true

    ###*
    @returns {Boolean} true if supplied object is array
    ###
    @typeIsArray = Array.isArray || ( value ) -> return {}.toString.call( value ) is '[object Array]'



###*
Class handling the events. Intendet to be used as base for extending other classes
###
class EventEmitter

    ###*
    Register callback on events
    @param {String} events Whitespace separated names of events
    @param {Function} callback Callback registred on the events
    @param {Object} cbThis Pointer to object that will be passed as 'this' to event callback
    ###
    on: (events, callback, cbThis) ->
        Utils.checkArgs('sf', 'EventEmitter.on()', arguments)
        eventList = this._getEvents(events)
        for event in eventList
            this._addCallback(event, callback, cbThis)
        return this


    ###*
    Unregister callbac on events
    @param {String} events Whitespace separated names of events
    @param {Function} callback Callback unregistred off the events
    ###
    off: (events, callback) ->
        Utils.checkArgs('sf', 'EventEmitter.off()', arguments)
        eventList = this._getEvents(events)
        for event in eventList
            this._removeCallback(event, callback)
        return this


    ###*
    Fire events with arguments
    @param {String} events Whitespace separated names of events
    @param {arguments} args... Variable number of arguments that will be passed to callback as arguments
    ###
    fire: (events, args...) ->
        Utils.checkArgs('s', 'EventEmitter.fire()', arguments)
        eventList = this._getEvents(events)
        for event in eventList
            this._callCallbacks(event, args)
        return this



    ###*
    Parse string containing whitespace-separated event names and return array with individual event names
    @param {String} eventsString Whitespace separated names of events
    ###
    _getEvents: (eventsString) -> eventsString.split(/\s+/)

    ###*
    Add callback to event
    @param {String} event Name of event for callbac to register to
    @param {Function} callback Callback we are registering to the event
    @param {Object} cbThis Pointer to object that will be passed as 'this' to event callback
    ###
    _addCallback: (event, callback, cbThis) ->
        # todo check if callback is already registred to this object and event
        if not this.listeners? then this.listeners = {}
        if not this.listeners.hasOwnProperty(event) then this.listeners[event] = []
        this.listeners[event].push( {func: callback, self: cbThis} )

    ###*
    Remove callback from event. If callback is registered to the event multiple times, it will remove all of them.
    @param {String} event Name of the event
    @param {Function} callback Callback we are unregistering
    ###
    _removeCallback: (event, callback) ->
        # no callbacks for event -> nothing to remove
        if not this.listeners? then return
        if not this.listeners.hasOwnProperty(event) then return
        Utils.removeAll(this.listeners[event], (item) -> item.func == callback)

    ###*
    Call all listeners for given event. Call with 'self' object passed as 'this' pointer and array 'args' as arguments.
    @param {String} event Name of the event
    @param {Object} self Pointer to object that will be passed as 'this' to event callback
    @param {Array} args Array of arguments that will be passed to callback as arguments
    ###
    _callCallbacks: (event, args) ->
        # no listeners at all or no callbacks for the event? Nothing to call!
        if not this.listeners? then return
        if not this.listeners.hasOwnProperty(event) then return

        for listener in this.listeners[event]
            self = listener.self
            callback = listener.func
            # try catch to prevent errors in callbacks to end the loop
            try callback.apply(self, args)
            catch err then console.error(err.stack)

class MessengerNotification extends EventEmitter

    ###*
    Add a new entry to the registry
    @param {Object} options The notification properties
    ###
    add: (options) ->
        cordova.exec(null , null, 'MessengerNotification', 'add', [options])

    ###*
    Cancels the specified notification
    @param {Object} options Tag of the notification ('messages', 'waypoints', ...)
    ###
    cancel: (tag) ->
        cordova.exec(null, null, 'MessengerNotification', 'cancel', [tag])

    ###*
    Removes all previously registered notifications.
    ###
    canceAll: () ->
        cordova.exec(null, null, 'MessengerNotification', 'cancelAll', [])


module.exports = new MessengerNotification()
