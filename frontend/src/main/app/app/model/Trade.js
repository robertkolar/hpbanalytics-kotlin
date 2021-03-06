/**
 * Created by robertk on 9/6/2015.
 */
Ext.define('HanGui.model.Trade', {
    extend: 'HanGui.model.Base',

    fields: [
        'type',
        'symbol',
        'underlying',
        'currency',
        'secType',
        'cumulativeQuantity',
        'status',
        'openPosition',
        'avgOpenPrice',
        'openDate',
        'avgClosePrice',
        'closeDate',
        'profitLoss',
        'timeValueSum',
        'valueSum',
        'executionIds'
    ]
});
