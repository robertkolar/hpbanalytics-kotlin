/**
 * Created by robertk on 9/6/2015.
 */
Ext.define('HanGui.common.Definitions', {
    statics: {
        urlPrefix: 'http://' + window.location.host,
        wsPrefix: 'ws://' + window.location.host + "/websocket",
        currencies: ['USD', 'EUR', 'CHF', 'GBP', 'JPY', 'AUD', 'KRW', 'HKD', 'SGD'],
        secTypes: ['OPT', 'FOP', 'FUT', 'CFD', 'STK']
    }
});
