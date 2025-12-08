export interface Link {
  href: string;
  templated?: boolean;
}

export interface Hateoas {
  _links: {
    [key: string]: Link;
  };
}

export interface PageInfo {
  size: number;
  totalElements: number;
  totalPages: number;
  number: number;
}
