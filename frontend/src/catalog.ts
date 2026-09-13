/* 상품 데이터는 백엔드(/api/products)에서 받는다. 이 파일에는 화면에서 쓰는 타입과 카테고리 목록만 남겨 둔다. */

export type Tier = "red" | "purple" | "yellow" | "green";

export type Product = {
  id: string;
  name: string;
  category: string;
  sub: string;
  price: number;
  tier: Tier;
  img: string;
  badge?: string;
  desc: string;
  specs: [string, string][];
};

export const NAV_ITEMS = ["Guns", "Weapons", "Bombs", "Gear", "Ammo"];

export const SUBCATS: Record<string, string[]> = {
  Guns: ["전체", "Pistol", "Revolver", "Machine Pistol", "Machine Gun", "SMG", "Rifle", "Sniper Rifle", "Shotgun", "Air Gun"],
  Weapons: ["전체", "Knife", "Sword", "Axe", "Baton", "Spear"],
  Bombs: ["전체", "Smoke Grenade", "Flash Bang", "Frag Grenade", "Claymore", "C4"],
  Gear: ["전체", "Vest", "Helmet", "Gloves", "Boots", "Night Vision"],
  Ammo: ["전체", "6mm BB", "8mm BB", "CO2 Cartridge", "Gas Can", "Tracer BB"],
};
