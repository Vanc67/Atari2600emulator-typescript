/* Avito Years 8.0: неразрушающее добавление года к старым и новым лотам. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || Game.prototype.avitoYearsLoaded) return;
  const proto = Game.prototype;
  function stamp(game, listing) {
    if (!listing || listing.year != null && Number.isFinite(Number(listing.year))) return;
    const details = game.listingDetails(listing);
    if (details && details.year) listing.year = details.year;
  }
  const baseGenerate = proto.generateListingSet;
  proto.generateListingSet = function () {
    const listings = baseGenerate.call(this);
    listings.forEach(listing => stamp(this, listing));
    return listings;
  };
  const baseNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseNormalize.call(this);
    if (Array.isArray(this.state.listings)) this.state.listings.forEach(listing => stamp(this, listing));
    return result;
  };
  proto.avitoYearsLoaded = true;
})();
